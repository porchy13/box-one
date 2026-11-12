/*
 * The map of Box One (MapView, DESIGN.md §14). The page only shows: Java calls the functions of window.boxOne, and the
 * clicks on the map, the markers dropped and the context menus asked go to the bridge window.bridge, installed by Java
 * (AGENTS.md §3). The texts handed by Java are written as text (textContent), never as HTML; no eval.
 */
'use strict';
(function () {
  /** The centre of Switzerland and the zoom of the first view. */
  var SWITZERLAND = [46.8, 8.23];
  var FIRST_ZOOM = 8;
  /** The size of the markers of the start and the finish, in pixels. */
  var MARKER = 28;
  /** The margin around the route when the map shows it whole, in pixels. */
  var MARGIN = 48;
  /** The scroll that changes the zoom by one level until Java sets it, in pixels (ZoomSensitivity.MEDIUM). */
  var WHEEL_PIXELS = 30;

  /*
   * The zoom of the wheel of Leaflet zooms by one level at least at each burst of scrolls, which a trackpad sends
   * many of; its fractional levels need the 3D transforms, turned off (setup.js). The page zooms itself instead: the
   * scrolls add up, and each time they reach the scroll of a level, the zoom changes by one level around the pointer.
   */
  var map = L.map('map', {zoomControl: true, scrollWheelZoom: false}).setView(SWITZERLAND, FIRST_ZOOM);
  var wheel = {pixels: WHEEL_PIXELS, total: 0};
  map.attributionControl.setPrefix(false);
  var tiles = null;
  var shown = L.layerGroup().addTo(map);
  var routes = [];

  /** An element holding a text. */
  function text(value) {
    var element = document.createElement('div');
    element.textContent = value;
    return element;
  }

  /**
   * A marker: a letter in a disc, with its meaning as tooltip; it can be dragged, and where it is dropped goes to the
   * bridge, as the start or the finish (its kind, start or finish).
   */
  function marker(latitude, longitude, letter, meaning, kind) {
    var icon = L.divIcon({className: 'marker', html: text(letter), iconSize: [MARKER, MARKER]});
    return L.marker([latitude, longitude], {icon: icon, keyboard: false, title: meaning, draggable: true, kind: kind})
      .bindTooltip(text(meaning))
      .on('dragend', function (event) {
        var place = event.target.getLatLng().wrap();
        if (window.bridge) {
          if (kind === 'start') {
            window.bridge.startMoved(place.lat, place.lng);
          } else {
            window.bridge.finishMoved(place.lat, place.lng);
          }
        }
      });
  }

  window.boxOne = {
    map: map,

    /** Sets a token of the theme: its name in the style sheets of JavaFX (-c-…), its value. */
    setColor: function (name, value) {
      document.documentElement.style.setProperty('-' + name, value);
    },

    /** Sets the tiles of the background map; the key fills the placeholder {key} of the address. */
    setTiles: function (url, attribution, maxZoom, key, routes) {
      if (tiles !== null) {
        map.removeLayer(tiles);
      }
      tiles = L.tileLayer(url, {maxZoom: maxZoom, key: key, attribution: attribution + ' | ' + routes});
      tiles.addTo(map);
    },

    /** Sets the scroll of the wheel or of the trackpad that changes the zoom by one level, in pixels. */
    setWheel: function (pixels) {
      wheel.pixels = pixels;
      wheel.total = 0;
    },

    /** Returns the scroll of the wheel or of the trackpad that changes the zoom by one level, in pixels. */
    getWheel: function () {
      return wheel.pixels;
    },

    /** Removes the markers and the routes. */
    clear: function () {
      shown.clearLayers();
      routes = [];
    },

    /** Shows the marker of the start or of the finish (its kind: start or finish). */
    addMarker: function (latitude, longitude, letter, meaning, kind) {
      shown.addLayer(marker(latitude, longitude, letter, meaning, kind));
    },

    /**
     * Shows a route: its points, as a JSON array of [latitude, longitude], and the style class of its line
     * (route-fastest, route-detour), drawn over the routes shown before.
     */
    addRoute: function (points, className) {
      var route = L.polyline(JSON.parse(points), {className: className});
      routes.push(route);
      shown.addLayer(route);
    },

    /** Shows the whole routes. */
    fit: function () {
      var bounds = L.latLngBounds([]);
      routes.forEach(function (route) {
        bounds.extend(route.getBounds());
      });
      map.fitBounds(bounds, {padding: [MARGIN, MARGIN]});
    }
  };

  map.getContainer().addEventListener('wheel', function (event) {
    event.preventDefault();
    var delta = L.DomEvent.getWheelDelta(event);
    if (wheel.total * delta < 0) {
      wheel.total = 0;
    }
    wheel.total += delta;
    var levels = Math.trunc(wheel.total / wheel.pixels);
    if (levels !== 0) {
      wheel.total -= levels * wheel.pixels;
      map.setZoomAround(map.mouseEventToContainerPoint(event), map.getZoom() + levels);
    }
  }, {passive: false});

  map.on('click', function (event) {
    var place = event.latlng.wrap();
    if (window.bridge) {
      window.bridge.clicked(place.lat, place.lng);
    }
  });

  /* A right-click asks Java for the context menu of the place, shown where the pointer is. */
  map.on('contextmenu', function (event) {
    var place = event.latlng.wrap();
    if (window.bridge) {
      window.bridge.menu(place.lat, place.lng, event.containerPoint.x, event.containerPoint.y);
    }
  });
}());
