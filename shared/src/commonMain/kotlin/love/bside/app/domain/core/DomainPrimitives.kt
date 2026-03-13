package love.bside.app.domain.core

import love.bside.app.core.UuidUtils
import love.bside.app.core.EntityId
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Base class for all domain aggregate roots.
 * Aggregates are consistency boundaries with their own identity and lifecycle.
 */
abstract class AggregateRoot<ID : Any> {
    abstract val id: ID
    abstract val version: Long
    abstract val createdAt: Instant
    abstract val updatedAt: Instant

    private val _domainEvents = mutableListOf<love.bside.app.orchestration.events.DomainEvent>()
    val domainEvents: List<love.bside.app.orchestration.events.DomainEvent> get() = _domainEvents.toList()

    protected fun addDomainEvent(event: love.bside.app.orchestration.events.DomainEvent) {
        _domainEvents.add(event)
    }

    fun clearDomainEvents() {
        _domainEvents.clear()
    }
}

/**
 * Base class for domain entities (have identity but not aggregate roots)
 */
abstract class Entity<ID : Any> {
    abstract val id: ID

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || this::class != other::class) {
            return false
        }
        other as Entity<*>
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

/**
 * Base class for value objects (no identity, compared by value)
 */
abstract class ValueObject {
    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int
}

/**
 * Specification pattern for complex business rules and queries
 */
interface Specification<T> {
    fun isSatisfiedBy(candidate: T): Boolean
    
    fun and(other: Specification<T>): Specification<T> = AndSpecification(this, other)
    fun or(other: Specification<T>): Specification<T> = OrSpecification(this, other)
    fun not(): Specification<T> = NotSpecification(this)
}

private class AndSpecification<T>(
    private val left: Specification<T>,
    private val right: Specification<T>
) : Specification<T> {
    override fun isSatisfiedBy(candidate: T): Boolean {
        return left.isSatisfiedBy(candidate) && right.isSatisfiedBy(candidate)
    }
}

private class OrSpecification<T>(
    private val left: Specification<T>,
    private val right: Specification<T>
) : Specification<T> {
    override fun isSatisfiedBy(candidate: T): Boolean {
        return left.isSatisfiedBy(candidate) || right.isSatisfiedBy(candidate)
    }
}

private class NotSpecification<T>(
    private val spec: Specification<T>
) : Specification<T> {
    override fun isSatisfiedBy(candidate: T): Boolean {
        return !spec.isSatisfiedBy(candidate)
    }
}

/**
 * Domain service marker interface
 */
interface DomainService

/**
 * Repository interface for aggregates
 */
interface Repository<T : AggregateRoot<ID>, ID : Any> {
    suspend fun findById(id: ID): T?
    suspend fun save(aggregate: T): T
    suspend fun delete(id: ID)
    suspend fun findAll(): List<T>
    suspend fun findBySpecification(spec: Specification<T>): List<T>
}
