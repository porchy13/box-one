/*
 * Options of Leaflet, read when leaflet.js loads (map.html loads this file first). The WebKit of JavaFX places the
 * layers moved with 3D transforms badly: tiles out of place, white gaps while moving or zooming. Leaflet then uses 2D
 * positions only.
 */
'use strict';
window.L_DISABLE_3D = true;
