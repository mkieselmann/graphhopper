package com.graphhopper.routing.template;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.coll.GHIntHashSet;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.RoutingAlgorithmFactory;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.weighting.TakeEdgesWeighting;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.Parameters.Algorithms.TrackRoute;
import com.graphhopper.util.PathMerger;
import com.graphhopper.util.Translation;
import com.graphhopper.util.shapes.GHPoint;

import java.util.List;

final public class TrackRoutingTemplate extends ViaRoutingTemplate {

    private final LocationIndex locationIndex;
    private GHIntHashSet trackEdgeIds;

    public TrackRoutingTemplate(GHRequest ghRequest, GHResponse ghRsp, LocationIndex locationIndex, EncodingManager encodingManager) {
        super(ghRequest, ghRsp, locationIndex, encodingManager);

        this.locationIndex = locationIndex;
    }

    @Override
    public List<QueryResult> lookup(List<GHPoint> points, FlagEncoder encoder) {
        if (points.size() > 2)
            throw new IllegalArgumentException("Currently alternative routes work only with start and end point. You tried to use: " + points.size() + " points");

        final String trackPoints = ghRequest.getHints().get(TrackRoute.TRACKPOINTS, "");
        EdgeFilter edgeFilter = DefaultEdgeFilter.allEdges(encoder);
        trackEdgeIds = parseTrackPointEdgeIds(trackPoints, edgeFilter);

        return super.lookup(points, encoder);
    }

    @Override
    public List<Path> calcPaths(QueryGraph queryGraph, RoutingAlgorithmFactory algoFactory, AlgorithmOptions algoOpts) {
        TakeEdgesWeighting takeEdgesWeighting = new TakeEdgesWeighting(algoOpts.getWeighting());
        takeEdgesWeighting.setEdgePenaltyFactor(5);
        takeEdgesWeighting.addEdges(trackEdgeIds);
        algoOpts = AlgorithmOptions.start(algoOpts).
                algorithm(Parameters.Algorithms.ASTAR_BI).
                weighting(takeEdgesWeighting).build();

        return super.calcPaths(queryGraph, algoFactory, algoOpts);
    }

    @Override
    public boolean isReady(PathMerger pathMerger, Translation tr) {
        return super.isReady(pathMerger, tr);
    }

    /**
     * This method reads the trackPointsString and creates a set of found edges.
     */
    public GHIntHashSet parseTrackPointEdgeIds(String trackPointString, EdgeFilter filter) {
        final String objectSeparator = ";";
        final String innerObjSep = ",";
        GHIntHashSet edges = new GHIntHashSet();

        if (!trackPointString.isEmpty()) {
            String[] trackPointCoordinatePairs = trackPointString.split(objectSeparator);
            for (int i = 0; i < trackPointCoordinatePairs.length; i++) {
                String coordinatePairAsString = trackPointCoordinatePairs[i];
                String[] splittedCoordinates = coordinatePairAsString.split(innerObjSep);
                if (splittedCoordinates.length == 2) {
                    double lat = Double.parseDouble(splittedCoordinates[0]);
                    double lon = Double.parseDouble(splittedCoordinates[1]);
                    QueryResult qr = locationIndex.findClosest(lat, lon, filter);
                    if (qr.isValid())
                        edges.add(qr.getClosestEdge().getEdge());
                } else {
                    throw new IllegalArgumentException(splittedCoordinates + " at index " + i + " need to be defined as lat,lon");
                }
            }
        }
        return edges;
    }
}
