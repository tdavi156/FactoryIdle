package com.github.jacks.factoryIdle.data

class GlobalResourcePool {
    private val pool = mutableMapOf<Resource, Float>()

    fun get(resource: Resource): Float = pool.getOrDefault(resource, 0f)
    fun add(resource: Resource, amount: Float) { pool[resource] = get(resource) + amount }
    fun subtract(resource: Resource, amount: Float) { pool[resource] = maxOf(0f, get(resource) - amount) }
    fun set(resource: Resource, amount: Float) { pool[resource] = amount }
    fun has(resource: Resource, amount: Float): Boolean = get(resource) >= amount
}

class LifetimeMiningStats {
    private val stats = mutableMapOf<Resource, Float>()

    fun add(resource: Resource, amount: Float) { stats[resource] = get(resource) + amount }
    fun get(resource: Resource): Float = stats.getOrDefault(resource, 0f)
}

class UnlockRegistry {
    private val resources = mutableSetOf<Resource>()
    private val buildingTypes = mutableSetOf<BuildingType>()

    fun unlock(resource: Resource) { resources.add(resource) }
    fun unlock(buildingType: BuildingType) { buildingTypes.add(buildingType) }
    fun isUnlocked(resource: Resource): Boolean = resource in resources
    fun isUnlocked(buildingType: BuildingType): Boolean = buildingType in buildingTypes
    fun unlockedResources(): Set<Resource> = resources
    fun unlockedBuildingTypes(): Set<BuildingType> = buildingTypes
}

class UnassignedPool {
    private val pool = mutableMapOf<BuildingType, Int>()

    fun add(type: BuildingType, count: Int) { pool[type] = pool.getOrDefault(type, 0) + count }
    fun remove(type: BuildingType, count: Int) {
        val current = pool.getOrDefault(type, 0)
        require(current >= count) { "Cannot remove $count of $type: only $current available" }
        pool[type] = current - count
    }
    fun count(type: BuildingType): Int = pool.getOrDefault(type, 0)
    fun canRemove(type: BuildingType, count: Int): Boolean = pool.getOrDefault(type, 0) >= count
}
