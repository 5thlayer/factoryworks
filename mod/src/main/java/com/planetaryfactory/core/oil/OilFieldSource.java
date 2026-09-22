package com.planetaryfactory.core.oil;

import java.util.List;

/** Implemented by the structure piece that places a crude-oil field (ADR-0081). */
public interface OilFieldSource {

    int centreX();

    int centreZ();

    /** Every well the field drew, before any is turned away by an ore column. */
    List<OilField.Well> wells();
}
