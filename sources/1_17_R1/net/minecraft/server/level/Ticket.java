package net.minecraft.server.level;

import java.util.Objects;

public final class Ticket<T> implements Comparable<Ticket<?>> {

    private final TicketType<T> type;
    private final int ticketLevel;
    public final T key;
    private long createdTick;

    protected Ticket(TicketType<T> tickettype, int i, T t0) {
        this.type = tickettype;
        this.ticketLevel = i;
        this.key = t0;
    }

    public int compareTo(Ticket<?> ticket) {
        int i = Integer.compare(this.ticketLevel, ticket.ticketLevel);

        if (i != 0) {
            return i;
        } else {
            int j = Integer.compare(System.identityHashCode(this.type), System.identityHashCode(ticket.type));

            return j != 0 ? j : this.type.a().compare(this.key, ticket.key);
        }
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof Ticket)) {
            return false;
        } else {
            Ticket<?> ticket = (Ticket) object;

            return this.ticketLevel == ticket.ticketLevel && Objects.equals(this.type, ticket.type) && Objects.equals(this.key, ticket.key);
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.type, this.ticketLevel, this.key});
    }

    public String toString() {
        return "Ticket[" + this.type + " " + this.ticketLevel + " (" + this.key + ")] at " + this.createdTick;
    }

    public TicketType<T> getTicketType() {
        return this.type;
    }

    public int b() {
        return this.ticketLevel;
    }

    protected void a(long i) {
        this.createdTick = i;
    }

    protected boolean b(long i) {
        long j = this.type.b();

        return j != 0L && i - this.createdTick > j;
    }
}
