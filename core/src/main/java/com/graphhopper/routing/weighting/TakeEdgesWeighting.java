package com.graphhopper.routing.weighting;

import com.graphhopper.coll.GHIntHashSet;
import com.graphhopper.util.EdgeIteratorState;

/**
 * Rates specific edges better.
 *
 * @author Robin Boldt
 */
public class TakeEdgesWeighting extends AbstractAdjustedWeighting {
    // contains the edge IDs of the edges that should be taken
    protected final GHIntHashSet takeEdges = new GHIntHashSet();

    private double edgePenaltyFactor = 5.0;

    public TakeEdgesWeighting(Weighting superWeighting) {
        super(superWeighting);
    }

    public TakeEdgesWeighting setEdgePenaltyFactor(double edgePenaltyFactor) {
        this.edgePenaltyFactor = edgePenaltyFactor;
        return this;
    }

    /**
     * This method adds the specified edge ids to this weighting which should be favored in the
     * calcWeight method.
     */
    public void addEdges(GHIntHashSet edgeIds) {
        takeEdges.addAll(edgeIds);
    }

    @Override
    public double getMinWeight(double distance) {
        return superWeighting.getMinWeight(distance);
    }

    @Override
    public double calcWeight(EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId) {
        double weight = superWeighting.calcWeight(edgeState, reverse, prevOrNextEdgeId);
        if (takeEdges.contains(edgeState.getEdge()))
            return 0.0;//return weight * edgePenaltyFactor;

        return weight;
    }

    @Override
    public String getName() {
        return "take_edges";
    }
}