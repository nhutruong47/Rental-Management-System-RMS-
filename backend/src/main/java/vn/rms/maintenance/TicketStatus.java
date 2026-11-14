package vn.rms.maintenance;

public enum TicketStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    ON_HOLD,
    RESOLVED,
    CLOSED,
    REOPENED;

    public boolean canTransitionTo(TicketStatus next) {
        return switch (this) {
            case OPEN -> next == ASSIGNED || next == IN_PROGRESS || next == CLOSED;
            case ASSIGNED -> next == IN_PROGRESS || next == ON_HOLD || next == RESOLVED || next == CLOSED;
            case IN_PROGRESS -> next == ON_HOLD || next == RESOLVED || next == CLOSED;
            case ON_HOLD -> next == IN_PROGRESS || next == CLOSED;
            case RESOLVED -> next == CLOSED || next == REOPENED;
            case REOPENED -> next == ASSIGNED || next == IN_PROGRESS || next == RESOLVED || next == CLOSED;
            case CLOSED -> next == REOPENED;
        };
    }
}
