package com.reborndw.flame;

/** The Dying Will form ladder. */
public enum Form {
    NONE, DYING_WILL, HYPER, ULTIMATE;

    public boolean boosted() { return this != NONE; }
}
