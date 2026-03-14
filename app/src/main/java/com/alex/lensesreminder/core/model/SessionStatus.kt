package com.alex.lensesreminder.core.model

/**
 * State machine for the lifetime of a lens wear session.
 */
enum class SessionStatus {
    PLANNED,
    ACTIVE,
    OVERDUE,
    COMPLETED,
    CANCELLED,
}
