package me.geeksploit.jointheflow.data;

public final class Flow {

    private String title;
    private long joinedCount;

    public Flow() {
    }

    public Flow(String title) {
        this.title = title;
        this.joinedCount = joinedCount;
    }

    public String getTitle() {
        return title;
    }

    public long getJoinedCount() {
        return joinedCount;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (! (other instanceof Flow)) return false;

        Flow otherFlow = (Flow) other;
        return this.getTitle().equals(otherFlow.getTitle());
    }

    @Override
    public int hashCode() {
        return getTitle().hashCode();
    }
}
