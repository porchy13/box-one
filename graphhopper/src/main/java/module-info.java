/**
 * GraphHopper, the routing engine of Box One, as one named module: the classes of {@code graphhopper-core},
 * {@code graphhopper-web-api} and {@code osm-legal-default-speeds-jvm}, regrouped because the first two share packages
 * and no module name can be derived from the third (see {@code graphhopper/pom.xml}).
 *
 * <p>Only the module {@code ch.varani.boxone.routing} reads it, behind its own interface (AGENTS.md §3). The module is
 * open and exports every package: GraphHopper reads its configuration with Jackson (reflection) and compiles its
 * custom models with Janino into classes of another class loader, which use its packages. The list of the packages is
 * the one of GraphHopper 11.1; it is checked again at every update of GraphHopper.</p>
 */
open module ch.varani.boxone.graphhopper {
  requires com.google.protobuf;
  requires hppc;
  requires jackson.datatype.jts;
  requires java.compiler;
  requires java.logging;
  requires org.apache.xmlgraphics.commons;
  requires org.codehaus.commons.compiler;
  requires org.codehaus.janino;
  requires org.locationtech.jts;
  requires osmosis.osm.binary;

  requires transitive com.fasterxml.jackson.annotation;
  requires transitive com.fasterxml.jackson.core;
  requires transitive com.fasterxml.jackson.databind;
  requires transitive com.fasterxml.jackson.dataformat.xml;
  requires transitive java.desktop;
  requires transitive java.xml;
  requires transitive kotlin.stdlib;
  requires transitive org.slf4j;

  exports com.graphhopper;
  exports com.graphhopper.apache.commons.collections;
  exports com.graphhopper.apache.commons.lang3;
  exports com.graphhopper.coll;
  exports com.graphhopper.config;
  exports com.graphhopper.debatty.java.stringsimilarity;
  exports com.graphhopper.geohash;
  exports com.graphhopper.isochrone.algorithm;
  exports com.graphhopper.jackson;
  exports com.graphhopper.json;
  exports com.graphhopper.reader;
  exports com.graphhopper.reader.dem;
  exports com.graphhopper.reader.osm;
  exports com.graphhopper.reader.osm.conditional;
  exports com.graphhopper.reader.osm.pbf;
  exports com.graphhopper.routing;
  exports com.graphhopper.routing.ch;
  exports com.graphhopper.routing.ev;
  exports com.graphhopper.routing.lm;
  exports com.graphhopper.routing.querygraph;
  exports com.graphhopper.routing.subnetwork;
  exports com.graphhopper.routing.util;
  exports com.graphhopper.routing.util.countryrules;
  exports com.graphhopper.routing.util.countryrules.europe;
  exports com.graphhopper.routing.util.parsers;
  exports com.graphhopper.routing.util.parsers.helpers;
  exports com.graphhopper.routing.util.tour;
  exports com.graphhopper.routing.weighting;
  exports com.graphhopper.routing.weighting.custom;
  exports com.graphhopper.search;
  exports com.graphhopper.storage;
  exports com.graphhopper.storage.index;
  exports com.graphhopper.util;
  exports com.graphhopper.util.details;
  exports com.graphhopper.util.exceptions;
  exports com.graphhopper.util.shapes;
  exports de.westnordost.osm_legal_default_speeds;
  exports de.westnordost.osm_legal_default_speeds.tagfilter;
  exports de.westnordost.osm_legal_default_speeds.tagfilter.filters;
}
