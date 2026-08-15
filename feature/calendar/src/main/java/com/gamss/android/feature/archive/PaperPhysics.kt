package com.gamss.android.feature.archive

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

internal fun Float.toRadians(): Float = this * (PI.toFloat() / 180f)
internal fun Float.toDegrees(): Float = this * (180f / PI.toFloat())

internal class PaperBody(
    startX: Float,
    startY: Float,
    startAngle: Float,
    val radius: Float,
    startVelX: Float = 0f,
    startVelY: Float = 0f,
    startAngularVelocity: Float = 0f,
) {
    var x: Float = startX
    var y: Float = startY
    var velX: Float = startVelX
    var velY: Float = startVelY
    var angle: Float = startAngle
    var angularVelocity: Float = startAngularVelocity
}

/**
 * 원(circle) 충돌 근사 기반의 경량 2D 리지드바디 시뮬레이터.
 * 카드가 중력으로 낙하하다 바닥/벽/서로에게 부딪혀 자연스럽게 쌓이는 연출용.
 *
 * 위치/속도를 불변 Vec2 대신 [PaperBody]의 원시 Float 필드로 들고, resolvePair()의 로컬 계산도
 * Float 변수로 풀어 쓴다 — 카드 20여 개 기준 프레임당 수천 개씩 생기던 임시 객체 할당을 없애 GC 압박을 줄인다.
 */
internal class PaperPhysicsWorld(
    private val bodies: List<PaperBody>,
    private val boundsWidth: Float,
    private val boundsHeight: Float,
) {
    fun step(dt: Float) {
        bodies.forEach { body ->
            body.velY += Gravity * dt
            body.x += body.velX * dt
            body.y += body.velY * dt
            body.angle += body.angularVelocity * dt
            body.velX *= LinearDamping
            body.velY *= LinearDamping
            val dampedAngularVelocity = (body.angularVelocity * AngularDamping).coerceIn(-MaxAngularVelocity, MaxAngularVelocity)
            // Snap near-zero spin to exactly zero, otherwise resting contacts keep re-injecting
            // sub-threshold torque every frame and the pile never stops turning in place.
            body.angularVelocity = if (abs(dampedAngularVelocity) < AngularSleepThreshold) 0f else dampedAngularVelocity
        }
        bodies.forEach(::resolveBounds)
        repeat(CollisionIterations) { resolvePairs() }
    }

    fun maxActivity(): Float = bodies.maxOf { body ->
        val linear = sqrt(body.velX * body.velX + body.velY * body.velY)
        val angular = abs(body.angularVelocity) * AngularActivityWeight
        linear + angular
    }

    private fun resolveBounds(body: PaperBody) {
        val minX = body.radius
        val maxX = boundsWidth - body.radius
        val floorY = boundsHeight - body.radius

        when {
            body.x < minX -> {
                body.x = minX
                body.velX = -body.velX * Restitution
                body.angularVelocity -= body.velY * WallSpinTransfer
            }

            body.x > maxX -> {
                body.x = maxX
                body.velX = -body.velX * Restitution
                body.angularVelocity += body.velY * WallSpinTransfer
            }
        }

        if (body.y > floorY) {
            body.y = floorY
            body.velY = if (body.velY > 0f) -body.velY * Restitution else body.velY
            val friction = body.velX * Friction
            body.velX = friction
            body.angularVelocity += friction * FloorSpinTransfer
        }
    }

    private fun resolvePairs() {
        for (i in bodies.indices) {
            for (j in i + 1 until bodies.size) {
                resolvePair(bodies[i], bodies[j])
            }
        }
    }

    private fun resolvePair(a: PaperBody, b: PaperBody) {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val distance = sqrt(dx * dx + dy * dy)
        val minDistance = a.radius + b.radius
        if (distance >= minDistance || distance <= MinSeparationDistance) return

        val nx = dx / distance
        val ny = dy / distance
        val overlap = (minDistance - distance - PositionSlop).coerceAtLeast(0f)
        val correctionX = nx * (overlap * 0.5f)
        val correctionY = ny * (overlap * 0.5f)
        a.x -= correctionX
        a.y -= correctionY
        b.x += correctionX
        b.y += correctionY

        val relVelX = b.velX - a.velX
        val relVelY = b.velY - a.velY
        val speedAlongNormal = relVelX * nx + relVelY * ny
        if (speedAlongNormal > 0f) return

        val impulseMagnitude = -(1f + Restitution) * speedAlongNormal / 2f
        val impulseX = nx * impulseMagnitude
        val impulseY = ny * impulseMagnitude
        a.velX -= impulseX
        a.velY -= impulseY
        b.velX += impulseX
        b.velY += impulseY

        val tangentX = -ny
        val tangentY = nx
        val tangentSpeed = relVelX * tangentX + relVelY * tangentY
        // Resting bodies keep re-touching under gravity every frame with tiny tangential noise;
        // ignoring it below this speed stops the pile from spinning in place once it has landed.
        if (abs(tangentSpeed) > RestingTangentSpeed) {
            val spinImpulse = tangentSpeed * PairSpinTransfer
            a.angularVelocity -= spinImpulse
            b.angularVelocity += spinImpulse
        }
    }

    // 아래 상수는 대부분 실기기(갤럭시 S23)로 직접 눈으로 보면서 맞춘 경험적 튜닝값이다.
    // 단위는 px/s, px/s², rad/s 등 시뮬레이션 내부 단위 기준이며, 다른 값으로 바꾸면 반드시
    // 실기기에서 낙하 애니메이션을 재확인해야 한다.
    private companion object {
        const val Gravity = 1700f
        const val LinearDamping = 0.995f
        const val AngularDamping = 0.9f
        const val Restitution = 0.32f
        const val Friction = 0.9f
        const val CollisionIterations = 4

        // Kept deliberately tiny: cards should mostly keep the tilt they spawned with and only
        // wobble a little on impact, not visibly spin while falling or colliding.
        const val PairSpinTransfer = 0.00012f
        const val WallSpinTransfer = 0.0001f
        const val FloorSpinTransfer = 0.0002f
        const val MaxAngularVelocity = 0.8f
        const val MinSeparationDistance = 1e-4f
        const val AngularActivityWeight = 40f
        const val AngularSleepThreshold = 0.05f
        const val RestingTangentSpeed = 8f
        const val PositionSlop = 0.5f
    }
}
