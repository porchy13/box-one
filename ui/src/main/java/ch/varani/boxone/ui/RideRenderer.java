package ch.varani.boxone.ui;

import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.WaypointKind;
import ch.varani.boxone.training.Ride;
import ch.varani.boxone.training.RideItem;
import ch.varani.boxone.training.RideJunction;
import ch.varani.boxone.training.RideRoad;
import ch.varani.boxone.training.Rider;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Draws the simulated run in 8 bits (development plan, phase 7c; asked by the maintainer on 2026-11-21: as the arcade
 * games of the time, nearly the rider's view, on a road faithful to the tulips): the sky and the hills, the ground in
 * perspective with the road, the roads around the junction ahead, the elements of the scene, and the motorbike seen
 * from behind, leaning in the turns, and its instruments.
 *
 * <p><b>Algorithm</b>: the ground is seen from a camera {@value #EYE} m above it, {@value #BEHIND} m behind the
 * motorbike, looking along its heading; a point of the ground ahead of the camera is projected at {@code x = middle +
 * side × focal / ahead}, {@code y = horizon + (eye − height) × focal / ahead} (the focal length {@value #FOCAL} of the
 * width of the canvas, of {@value #WIDEST} times its height at most; the camera higher on 2026-11-22, asked by the
 * maintainer, to see the roads farther), a polygon of the ground cut where it passes behind the camera. The road is
 * drawn from {@value #BEHIND_ROAD} m behind the motorbike to {@value #AHEAD} m ahead (asked by the maintainer on
 * 2026-11-21: the roads beyond a junction seen before reaching it). So that the road does not tell the branch to take,
 * beyond the junction ahead it looks as the other branches of that junction, plain, without middle line nor bands of
 * grass. With the map, the branches are the roads of the map within {@value Ride#SURROUNDINGS} m of the junction, as
 * they bend, and the roads meeting the road ridden ({@link Ride#mapRoads()}; asked by the maintainer on 2026-11-21 and
 * 2026-11-22), the route beyond the junction and the ring of a roundabout among them, each drawn once a point of it is
 * within {@value #AHEAD} m of the motorbike ({@link #inSight}; asked by the maintainer on 2026-11-22: the roads came
 * too late), and never thinner on the canvas than {@value #ROAD_LINE} of its height (a road across the view 100 m ahead
 * is a pixel thick: asked by the maintainer on 2026-11-22, the roads seen only at the pin), that line under its white
 * edges, a tarmac road with the dashed middle line of the road ridden (asked by the maintainer on 2026-11-22: the white
 * lines came only once the junction before was passed). Without it, each branch of the tulip but the exit is drawn
 * straight, {@value #BRANCH} m long from the junction (from the ring of a roundabout, fitted to the route and drawn
 * whole with its island), and the road beyond the junction as far: a straight exit would leave a ghost of a road that
 * bends. A tarmac road is grey between white edges, a track brown without edges (asked by the maintainer on 2026-11-21:
 * no red and white kerbs, no posts, the tracks in a colour of their own), the kind of the road ridden told by the
 * tulips ({@link Ride#kind(double)}), of a road of the map by the map, of a branch by its tulip. Layer by layer, from
 * the farthest to the nearest in each, so that a road crossing or meeting another one is never cut by its edges: bands
 * of grass across {@value #GRASS} m either side of the road, which show the speed, up to the junction ahead; the edges
 * of every road; every road, the one ridden in bands of two shades up to the junction ahead; the dashed middle line of
 * a tarmac road; the island of a roundabout; the obstacles noted in the roadbook in relief, a mound across the road, a
 * hole in its middle ({@link Ride#items()}; asked by the maintainer on 2026-11-21 and 2026-11-22), up to the junction
 * ahead, or to the road drawn once the motorbike is past the junction along it; the finish line; then, upright, from
 * the farthest to the nearest, the elements beside the road noted in the roadbook, a tree, a house or a castle, and the
 * trees not noted at the roadside, not all the time ({@link #trees}), sprites of pixels scaled by their distance
 * ({@link Billboard}), and the beacons of the waypoints marked in the roadbook at the right of the road, a post and a
 * disc in their colours ({@link Beacon}; asked by the maintainer on 2026-11-21). The hills turn with the heading. The
 * motorbike is a sprite of pixels ({@link #MOTO}), each a square of the size of the canvas. Last, the instruments in
 * pixels (asked by the maintainer on 2026-11-22: the speed and the trip in the game): the speedometer at the bottom
 * left, a gauge and the speed in figures, and the trip meter of a rally at the bottom right.</p>
 *
 * <p>Utility class without state; confined to the JavaFX Application Thread (it draws on a canvas).</p>
 */
final class RideRenderer {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /**
   * The height of the camera above the ground, in metres: high enough to read a junction far ahead (asked by the
   * maintainer on 2026-11-22; 12 m chosen by the maintainer the same day after a comparison: from 6 m, the junction of
   * the next box, 125 m ahead, was a line under the horizon, read too late at speed).
   */
  static final double EYE = 12;
  /**
   * The distance of the camera behind the motorbike, in metres: twice its height, the motorbike near the bottom of the
   * canvas.
   */
  static final double BEHIND = 24;
  /** The focal length, as a share of the width of the canvas. */
  static final double FOCAL = 0.9;
  /**
   * The widest canvas the focal length follows, as a share of its height: a wider canvas sees more at its sides, the
   * motorbike kept above its bottom.
   */
  static final double WIDEST = 4.0 / 3;
  /** The height of the horizon, as a share of the height of the canvas: the higher camera looks down on more road. */
  static final double HORIZON = 0.3;
  /** The nearest distance ahead of the camera drawn, in metres: a polygon is cut there. */
  static final double NEAR = 0.5;
  /** The distance of road drawn behind the motorbike, in metres: under the camera. */
  static final double BEHIND_ROAD = 15;
  /** The distance of road drawn ahead of the motorbike, in metres. */
  static final double AHEAD = 320;
  /** Half the width of the road, in metres. */
  static final double ROAD = 3.5;
  /**
   * The least thickness of a road of the map on the canvas, a share of its height: chosen for Box One, a road far
   * ahead seen as a line, 4 pixels on a canvas 600 pixels high.
   */
  static final double ROAD_LINE = 0.007;
  /** The width of the white edge of a tarmac road, in metres. */
  static final double EDGE = 0.5;
  /** Half the width of the middle line, in metres. */
  static final double LANE = 0.12;
  /** The length of a dash of the middle line, and of the gap after it, in metres. */
  static final double DASH = 3;
  /** The length of a band of the grass and of the road, in metres: the bands show the speed. */
  static final double BAND = 6;
  /**
   * The overlap of a band on the band beyond, in metres: the nearer band covers the edge of the farther one, without
   * the seam the smoothing of their edges would leave.
   */
  static final double OVERLAP = 0.3;
  /** The distance of grass drawn either side of the road, in metres. */
  static final double GRASS = 45;
  /** The length of a branch of a junction drawn, in metres: as far as the road beyond it is seen when it is near. */
  static final double BRANCH = 120;
  /** The step of the polygons of a branch, in metres. */
  static final double BRANCH_STEP = 5;
  /** The polygons of the ring of a roundabout. */
  static final int RING_SEGMENTS = 36;
  /** The length of the squares of the finish line, in metres. */
  static final double FINISH = 1;
  /** The height of the hills, as a share of the height of the canvas. */
  static final double HILLS = 0.08;
  /** The width of a step of the hills, in pixels: their outline in steps, as in 8 bits. */
  static final double HILL_STEP = 8;
  /** The width of a hill, in pixels. */
  static final double HILL_WIDTH = 160;
  /** The shift of the hills per degree of heading, in pixels. */
  static final double HILL_TURN = 6;
  /**
   * The height of the canvas per pixel of the sprite of the motorbike: the size of its pixels, the motorbike as wide
   * against the road seen from {@value #BEHIND} m behind as it was from 12 m (asked by the maintainer on 2026-11-22: it
   * did not follow the camera).
   */
  static final double SPRITE_SCALE = 320;
  /** The largest lean of the motorbike in a turn, in degrees. */
  static final double MAX_LEAN = 15;
  /** The speed at which the motorbike leans fully, in km/h. */
  static final double LEAN_SPEED = 40;
  /** The metres of a pixel of the sprite of a tree: about 7 m high. */
  static final double TREE_PIXEL = 0.5;
  /** The metres of a pixel of the sprite of a house: about 10 m wide. */
  static final double HOUSE_PIXEL = 0.7;
  /** The metres of a pixel of the sprite of a castle: about 18 m wide. */
  static final double CASTLE_PIXEL = 0.9;
  /** The length of a bump across the road, in metres. */
  static final double BUMP = 3;
  /** The height of a bump across the road at its crest, in metres. */
  static final double BUMP_HEIGHT = 0.8;
  /** The length of the shadow of a bump on the road beyond it, in metres. */
  static final double BUMP_SHADOW = 1.5;
  /** The radius of a hole, in metres. */
  static final double HOLE = 1.5;
  /** The radius of the broken edge around a hole, as a share of its radius. */
  static final double HOLE_RIM = 1.3;
  /** The depth of a hole, in metres: its far wall seen. */
  static final double HOLE_DEPTH = 0.3;
  /** The width of a hole across the road, as a share of its length: wider than long. */
  static final double HOLE_WIDE = 1.5;
  /** The sides of the polygon of a hole. */
  static final int HOLE_SIDES = 16;
  /** The bands between two trees along the road, at most: the trees not noted, at the roadside, not all the time. */
  static final int TREE_BANDS = 5;
  /** The distance of a tree not noted from the middle of the road, in metres. */
  static final double TREE_SIDE = 12;
  /** The distance from a junction under which no tree not noted stands, in metres: off its roads. */
  static final double TREE_CLEAR = 40;
  /** The draws of a tree not noted: one in so many at the left, as many at the right, none otherwise. */
  static final int TREE_DRAWS = 5;
  /** The prime that mixes the index of a band into the draw of its tree. */
  static final int TREE_MIX = 7919;
  /**
   * The overlap of a pixel of a sprite on its neighbours, in pixels of the canvas: without it, the smoothing of their
   * edges leaves seams between the pixels of a sprite scaled by its distance.
   */
  static final double SEAM = 0.5;
  /** The distance of the beacon of a waypoint from the middle of the road, in metres: at its right. */
  static final double BEACON_SIDE = 6;
  /** The height of the beacon of a waypoint, to the middle of its disc, in metres. */
  static final double BEACON_HEIGHT = 2.5;
  /** The diameter of the disc of the beacon of a waypoint, in metres. */
  static final double BEACON_DISC = 1.4;
  /** The width of the post of a beacon, as a share of its disc. */
  static final double BEACON_POST = 0.12;
  /** The width of the black ring around the disc of a beacon, as a share of its disc. */
  static final double BEACON_RING = 0.06;
  /** The size of the letters of a beacon, as a share of its disc, for two letters. */
  static final double BEACON_TEXT = 1.3;
  /** The height of the canvas per pixel of the instruments: the size of their pixels. */
  static final double DASH_SCALE = 120;
  /** The space between the instruments and the corners of the canvas, in their pixels. */
  static final int DASH_MARGIN = 3;
  /** The margin inside an instrument, in its pixels. */
  static final int DASH_PADDING = 2;
  /** The blocks of the gauge of the speed, from 0 to {@link Rider#MAX_SPEED}. */
  static final int GAUGE_BLOCKS = 13;
  /** The width of a block of the gauge, in pixels of the instruments, without the gap of one after it. */
  static final int GAUGE_BLOCK = 2;
  /** The height of the first block of the gauge, in pixels of the instruments; each pair of blocks one higher. */
  static final int GAUGE_LOW = 2;
  /** The figures of the speed. */
  static final int SPEED_FIGURES = 3;
  /** The figures of the kilometres of the trip, before its hundredths: it turns over after 999.99 km. */
  static final int TRIP_FIGURES = 3;
  /** The figures of the hundredths of the trip. */
  static final int TRIP_DECIMALS = 2;
  /** The metres in a hundredth of a kilometre: the trip of the roadbook. */
  static final double METRES_PER_HUNDREDTH = 10;
  /** The hundredths of a kilometre after which the trip meter turns over: 1000 km. */
  static final long TURN_OVER = 100_000;
  /** The format of the trip meter: its kilometres and its hundredths, the cells on the left filled with zeros. */
  private static final String TRIP_FORMAT = "%0" + (TRIP_FIGURES + TRIP_DECIMALS) + "d";
  /** The width of a figure, in pixels of the instruments ({@link #FIGURES}). */
  static final int FIGURE_WIDTH = 5;
  /** The height of a figure, in pixels of the instruments. */
  static final int FIGURE_HEIGHT = 7;
  /** The size of the letters of the units, in pixels of the instruments. */
  static final int UNIT_SIZE = 3;
  /** The figure drawn unlit behind each figure of the speed, as on a display of segments: the 8, every segment. */
  static final int GHOST = 8;
  /** The ten figures of the instruments, from 0 to 9, drawn for Box One: each X a pixel lit. */
  static final List<List<String>> FIGURES = figures("""
      .XXX. ..X.. .XXX. XXXXX ...X. XXXXX ..XX. XXXXX .XXX. .XXX.
      X...X .XX.. X...X ...X. ..XX. X.... .X... ....X X...X X...X
      X..XX ..X.. ....X ..X.. .X.X. XXXX. X.... ...X. X...X X...X
      X.X.X ..X.. ...X. ...X. X..X. ....X XXXX. ..X.. .XXX. .XXXX
      XX..X ..X.. ..X.. ....X XXXXX ....X X...X .X... X...X ....X
      X...X ..X.. .X... X...X ...X. X...X X...X .X... X...X ...X.
      .XXX. .XXX. XXXXX .XXX. ...X. .XXX. .XXX. .X... .XXX. .XX..
      """);
  /** The distance of the warning sign of an obstacle before it, in metres, at the right of the road. */
  static final double SIGN_AHEAD = 50;
  /** The side of the triangle of a warning sign, in metres. */
  static final double SIGN_SIDE = 2;
  /** The height of the post of a warning sign, to the bottom of its triangle, in metres. */
  static final double SIGN_HEIGHT = 2;
  /** The red border of a warning sign, as a share of the height of its triangle. */
  static final double SIGN_BORDER = 0.22;
  /** The width of the symbol of a warning sign, as a share of the side of its triangle. */
  static final double SIGN_SYMBOL = 0.42;
  /** The height of the triangle of a warning sign, as a share of its side: an equilateral triangle. */
  private static final double TRIANGLE = Math.sqrt(3) / 2;
  /** A half turn, in degrees: the extent of the half disc of the symbol of a warning sign. */
  private static final double HALF_TURN = 180;
  /** A full turn, in degrees. */
  private static final double FULL_TURN = 360;
  /** The index of the metres north of the point at the right of the road, in the result of {@link #side}. */
  private static final int RIGHT_NORTH = 3;
  /** A tree, drawn for Box One: its leaves and its trunk ({@link RidePalette#SPRITE_TOKENS}). */
  static final List<String> TREE = pixels("""
      ....LLLL....
      ..LLLLLLLL..
      .LLLLLLLLLL.
      LLLLLLLLLLLL
      LLLLLLLLLLLL
      .LLLLLLLLLL.
      LLLLLLLLLLLL
      LLLLLLLLLLLL
      .LLLLLLLLLL.
      ..LLLLLLLL..
      .....TT.....
      .....TT.....
      .....TT.....
      ....TTTT....
      """);
  /** A house, drawn for Box One: its red roof, its white walls, its windows and its door. */
  static final List<String> HOUSE = pixels("""
      ......RR......
      ....RRRRRR....
      ..RRRRRRRRRR..
      RRRRRRRRRRRRRR
      .WWWWWWWWWWWW.
      .WBBWWWWWWBBW.
      .WBBWWWWWWBBW.
      .WWWWWWWWWWWW.
      .WWWWWTTWWWWW.
      .WWWWWTTWWWWW.
      .WWWWWTTWWWWW.
      """);
  /** A castle, drawn for Box One: its two towers and its wall, its windows and its gate. */
  static final List<String> CASTLE = pixels("""
      GG.GG..........GG.GG
      GGGGG..........GGGGG
      GGGGG.G.G.G.G..GGGGG
      GGKGGGGGGGGGGGGGGKGG
      GGGGGGGGGGGGGGGGGGGG
      GGKGGGGGGGGGGGGGGKGG
      GGGGGGGGGKKGGGGGGGGG
      GGGGGGGGKKKKGGGGGGGG
      GGGGGGGGKKKKGGGGGGGG
      GGGGGGGGKKKKGGGGGGGG
      """);
  /**
   * The motorbike of rally and its rider, seen from behind (drawn for Box One; asked by the maintainer on 2026-11-22:
   * finer, as the motorbikes of the consoles of the time): each letter a pixel of a colour of
   * {@link RidePalette#SPRITE_TOKENS}, a dot transparent; the helmet, red with a white stripe, the rider in white with
   * the arms out to the handlebar and the mirrors, the legs either side of the tail, the plate with the number 1 (the
   * first box), the rear lights, the exhaust at the right and the tyre.
   */
  static final List<String> MOTO = pixels("""
      .............KKKKKK.............
      ...........KKRRWWRRKK...........
      ..........KRRRRWWRRRRK..........
      .........KRRRRRWWRRRRRK.........
      .........KRRRRRWWRRRRRK.........
      ........KDRRRRRWWRRRRRDK........
      ........KDRRRRRWWRRRRRDK........
      ........KDDRRRRWWRRRRDDK........
      .........KDDRRRRRRRRDDK.........
      ..........KKDDDDDDDDKK..........
      ...........KSSSSSSSSK...........
      ........KKKWWWWWWWWWWKKK........
      ......KKWWWWWWWWWWWWWWWWKK......
      .....KWWWWWWPWWRRWWPWWWWWWK.....
      ....KWWWWWWWPWWRRWWPWWWWWWWK....
      ...KWWPKWWWWPWWRRWWPWWWWKPWWK...
      ...KWPK.KWWWPWWRRWWPWWWK.KPWK...
      ..KWPK..KWWWWPWWWWPWWWWK..KPWK..
      ..KPK...KPWWWWPWWPWWWWPK...KPK..
      .KKKK...KPPWWWWWWWWWWPPK...KKKK.
      KKKKKSSSKKPPPPPPPPPPPPKKSSSKKKKK
      .KKKK..KWKDDRRRRRRRRDDKWK..KKKK.
      ..PG...KWKDRRRRRRRRRRDKWK...GP..
      .PPGG.KWWKDRWWWKKWWWRDKWWK.GGPP.
      ..GG..KWPKDRWWKKKWWWRDKPWK..GG..
      ......KWPKDRWWWKKWWWRDKPWK......
      ......KKPKDRWWWKKWWWRDKPKK......
      .......KKKDRWWKKKKWWRDKKK.......
      .........KDRRRRRRRRRRKPGK.......
      ..........KDDRRRRRRDDKPGK.......
      ..........KKOKRRRRKOKKPGK.......
      ...........KSSSSSSSSKKPGK.......
      ...........KKSKKKKSKKKPGK.......
      ...........KSKKKKKKSK.KK........
      ...........KKSKKKKSKK...........
      ...........KSKKKKKKSK...........
      ...........KKSKKKKSKK...........
      ...........KSKKKKKKSK...........
      ............KKKKKKKK............
      """);

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private RideRenderer() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Reads a drawing of pixels: one row per line.
   *
   * @param drawing the rows of the drawing, one per line, each letter a pixel.
   *
   * @return the rows, from the top.
   */
  private static List<String> pixels(final String drawing) {
    return drawing.lines().toList();
  }

  /**
   * Reads the figures of the instruments drawn side by side: each row of the drawing holds the same row of every
   * figure, {@value #FIGURE_WIDTH} pixels each, separated by a space.
   *
   * @param drawing the {@value #FIGURE_HEIGHT} rows of the figures, from 0 to 9.
   *
   * @return the rows of each figure, from 0 to 9.
   */
  private static List<List<String>> figures(final String drawing) {
    final List<String> rows = pixels(drawing);
    final int count = (rows.getFirst().length() + 1) / (FIGURE_WIDTH + 1);
    final List<List<String>> figures = new ArrayList<>();
    for (int figure = 0; figure < count; figure++) {
      final int start = figure * (FIGURE_WIDTH + 1);
      figures.add(rows.stream().map(row -> row.substring(start, start + FIGURE_WIDTH)).toList());
    }
    return List.copyOf(figures);
  }

  /**
   * Draws the run.
   *
   * @param graphics the graphics of the canvas.
   * @param width the width of the canvas, in pixels.
   * @param height the height of the canvas, in pixels.
   * @param rider the motorbike and its road.
   * @param steer the turn of the rider: −1 to the left, 1 to the right, 0 straight; the motorbike leans with it.
   * @param dash the texts of the instruments.
   * @param palette the colours.
   */
  static void draw(final GraphicsContext graphics, final double width, final double height, final Rider rider,
                   final int steer, final Dash dash, final RidePalette palette) {
    final Camera camera = Camera.of(rider, width, height);
    graphics.setImageSmoothing(false);
    graphics.setFill(palette.color(RidePalette.SKY));
    graphics.fillRect(0, 0, width, height);
    drawHills(graphics, width, camera, palette);
    final Ride ride = rider.ride();
    final Optional<RideJunction> ahead = rider.ahead();
    final double from = Math.max(0, rider.progress() - BEHIND_ROAD);
    final double end = Math.min(ride.length(), rider.progress() + AHEAD);
    final double to = Math.min(ahead.map(RideJunction::distance).orElse(ride.length()), end);
    drawGrass(graphics, camera, ride, from, to, palette);
    final boolean map = !ride.mapRoads().isEmpty();
    final List<RideJunction> tulips = map ? List.of() : ride.junctions().stream()
        .filter(junction -> junction.distance() > rider.progress() - BRANCH && junction.distance() <= to).toList();
    final List<RideRoad> roads = ride.mapRoads().stream().filter(road -> inSight(road, rider)).toList();
    final double beyond = map ? to : Math.min(end, to + BRANCH);
    tulips.forEach(junction -> drawBranches(graphics, camera, junction, true, palette));
    roads.forEach(road -> drawMapRoad(graphics, camera, road, true, palette));
    drawPlain(graphics, camera, ride, to, beyond, true, palette);
    drawEdges(graphics, camera, ride, from, to, palette);
    tulips.forEach(junction -> drawBranches(graphics, camera, junction, false, palette));
    roads.forEach(road -> drawMapRoad(graphics, camera, road, false, palette));
    roads.forEach(road -> drawMapLane(graphics, camera, road, palette));
    drawPlain(graphics, camera, ride, to, beyond, false, palette);
    tulips.forEach(junction -> drawRing(graphics, camera, junction, palette));
    drawSurface(graphics, camera, ride, from, to, palette);
    drawLane(graphics, camera, ride, from, to, palette);
    tulips.forEach(junction -> drawIsland(graphics, camera, junction, palette));
    final double seen = rider.progress() >= to ? end : to;
    final List<RideItem> items = ride.items().stream()
        .filter(item -> item.distance() >= from - BRANCH && item.distance() <= seen).toList();
    items.stream().filter(item -> item.kind().isObstacle())
         .forEach(item -> drawObstacle(graphics, camera, ride, item, palette));
    if (end >= ride.length()) {
      drawFinish(graphics, camera, ride, palette);
    }
    final List<Upright> uprights = new ArrayList<>(trees(camera, ride, from, to));
    items.stream().filter(item -> !item.kind().isObstacle())
         .forEach(item -> uprights.add(Billboard.of(camera, item.kind(), item.east(), item.north())));
    items.stream().filter(item -> item.kind().isObstacle()).forEach(item -> {
      final double[] sides = side(ride, Math.max(0, item.distance() - SIGN_AHEAD), BEACON_SIDE);
      uprights.add(new Warning(item.kind(), sides[2], sides[RIGHT_NORTH],
                               camera.view(sides[2], sides[RIGHT_NORTH])[0]));
    });
    ride.waypoints().stream()
        .filter(waypoint -> waypoint.waypoint().kind().isMarked() && waypoint.distance() >= from - BRANCH
                            && waypoint.distance() <= seen)
        .forEach(waypoint -> {
          final double[] sides = side(ride, waypoint.distance(), BEACON_SIDE);
          uprights.add(new Beacon(waypoint.waypoint().kind(), sides[2], sides[RIGHT_NORTH],
                                  camera.view(sides[2], sides[RIGHT_NORTH])[0]));
        });
    uprights.stream().filter(upright -> upright.ahead() >= NEAR)
            .sorted(Comparator.comparingDouble(Upright::ahead).reversed())
            .forEach(upright -> upright.draw(graphics, camera, palette));
    final double lean = steer * MAX_LEAN * Math.min(1, rider.speed() / LEAN_SPEED);
    final double pixel = Math.max(1, height / SPRITE_SCALE);
    drawSprite(graphics, MOTO, width / 2, camera.horizon() + EYE * camera.focal() / BEHIND, pixel, lean, palette);
    final double unit = Math.max(1, Math.floor(height / DASH_SCALE));
    drawSpeedometer(graphics, height, unit, rider, dash, palette);
    drawTripMeter(graphics, width, height, unit, rider.trip(), dash, palette);
  }

  /**
   * Draws the hills on the horizon, in steps, turned with the heading, and the grass under them.
   *
   * @param graphics the graphics of the canvas.
   * @param width the width of the canvas.
   * @param camera the camera.
   * @param palette the colours.
   */
  private static void drawHills(final GraphicsContext graphics, final double width, final Camera camera,
                                final RidePalette palette) {
    graphics.setFill(palette.color(RidePalette.HILL));
    final double shift = -camera.heading() * HILL_TURN;
    for (double x = 0; x < width; x += HILL_STEP) {
      final double phase = (x - shift) / HILL_WIDTH;
      final double rise = (Math.sin(phase) + Math.sin(phase * 2 + 1) / 2 + 1.5) / 3;
      final double top = Math.floor(rise * HILLS * camera.height() / HILL_STEP) * HILL_STEP;
      graphics.fillRect(x, camera.horizon() - top, HILL_STEP, top);
    }
    graphics.setFill(palette.color(RidePalette.GRASS));
    graphics.fillRect(0, camera.horizon(), width, camera.height() - camera.horizon());
  }

  /**
   * Draws the bands of grass along the road, from the farthest to the nearest.
   *
   * @param graphics the graphics of the canvas.
   * @param camera the camera.
   * @param ride the road.
   * @param from the distance of the nearest band.
   * @param to the distance of the farthest band.
   * @param palette the colours.
   */
  private static void drawGrass(final GraphicsContext graphics, final Camera camera, final Ride ride,
                                final double from, final double to, final RidePalette palette) {
    final int first = (int) Math.floor(from / BAND);
    for (int band = (int) Math.ceil(Math.min(to, ride.length()) / BAND) - 1; band >= first; band--) {
      graphics.setFill(palette.color(band % 2 == 0 ? RidePalette.GRASS_DARK : RidePalette.GRASS));
      strip(graphics, camera, ride, Math.max(from, band * BAND), (band + 1) * BAND + OVERLAP, GRASS);
    }
  }

  /**
   * Draws the white edges of the road ridden where it is a tarmac road, in bands that follow its bends, from the
   * farthest to the nearest.
   *
   * @param graphics the graphics of the canvas.
   * @param camera the camera.
   * @param ride the road.
   * @param from the distance where it begins.
   * @param to the distance where it ends.
   * @param palette the colours.
   */
  private static void drawEdges(final GraphicsContext graphics, final Camera camera, final Ride ride,
                                final double from, final double to, final RidePalette palette) {
    graphics.setFill(palette.color(RidePalette.EDGE));
    final int first = (int) Math.floor(from / BAND);
    for (int band = (int) Math.ceil(to / BAND) - 1; band >= first; band--) {
      final double start = Math.max(from, band * BAND);
      strip(graphics, camera, ride, start, Math.min(to, (band + 1) * BAND + OVERLAP), half(ride.kind(start), true));
    }
  }

  /**
   * Draws the road ridden, in bands of two shades of its kind, from the farthest to the nearest.
   *
   * @param graphics the graphics of the canvas.
   * @param camera the camera.
   * @param ride the road.
   * @param from the distance where it begins.
   * @param to the distance where it ends.
   * @param palette the colours.
   */
  private static void drawSurface(final GraphicsContext graphics, final Camera camera, final Ride ride,
                                  final double from, final double to, final RidePalette palette) {
    final int first = (int) Math.floor(from / BAND);
    for (int band = (int) Math.ceil(to / BAND) - 1; band >= first; band--) {
      final double start = Math.max(from, band * BAND);
      graphics.setFill(color(ride.kind(start), false, band % 2 == 0, palette));
      strip(graphics, camera, ride, start, Math.min(to, (band + 1) * BAND + OVERLAP), ROAD);
    }
  }

  /**
   * Draws the dashes of the middle line of the road ridden where it is a tarmac road, from the farthest to the
   * nearest.
   *
   * @param graphics the graphics of the canvas.
   * @param camera the camera.
   * @param ride the road.
   * @param from the distance where it begins.
   * @param to the distance where it ends.
   * @param palette the colours.
   */
  private static void drawLane(final GraphicsContext graphics, final Camera camera, final Ride ride,
                               final double from, final double to, final RidePalette palette) {
    graphics.setFill(palette.color(RidePalette.LANE));
    final int first = (int) Math.floor(from / DASH);
    for (int dash = (int) Math.ceil(to / DASH) - 1; dash >= first; dash--) {
      final double start = Math.max(from, dash * DASH);
      if (dash % 2 == 0 && ride.kind(start) == BranchKind.TARMAC) {
        strip(graphics, camera, ride, start, Math.min(to, (dash + 1) * DASH), LANE);
      }
    }
  }

  /**
   * Fills a strip of ground along the road between two distances, a half width either side of its middle.
   *
   * @param graphics the graphics of the canvas.
   * @param camera the camera.
   * @param ride the road.
   * @param from the distance of its near end.
   * @param to the distance of its far end.
   * @param half its half width, in metres.
   */
  private static void strip(final GraphicsContext graphics, final Camera camera, final Ride ride, final double from,
                            final double to, final double half) {
    final double[] near = side(ride, from, half);
    final double[] far = side(ride, to, half);
    camera.fill(graphics, new double[] {near[0], near[2], far[2], far[0]},
                new double[] {near[1], near[RIGHT_NORTH], far[RIGHT_NORTH], far[1]});
  }

  /**
   * Gives the points either side of the road at a distance.
   *
   * @param ride the road.
   * @param along the distance.
   * @param half the distance from the middle, in metres.
   *
   * @return the metres east and north of the point at the left, then of the point at the right.
   */
  private static double[] side(final Ride ride, final double along, final double half) {
    final double heading = Math.toRadians(ride.heading(along));
    final double rightEast = Math.cos(heading) * half;
    final double rightNorth = -Math.sin(heading) * half;
    final double east = ride.east(along);
    final double north = ride.north(along);
    return new double[] {east - rightEast, north - rightNorth, east + rightEast, north + rightNorth};
  }

  /**
   * Fills a stretch of the road ridden as a branch of a junction, its edges or its surface, in bands that follow its
   * bends, from the farthest to the nearest.
   *
   * @param graphics the graphics of the canvas.
   * @param camera the camera.
   * @param ride the road.
   * @param from the distance where the stretch begins.
   * @param to the distance where it ends; nothing is drawn if it is not beyond its beginning.
   * @param edge {@code true} for its edges, {@code false} for its surface.
   * @param palette the colours.
   */
  private static void drawPlain(final GraphicsContext graphics, final Camera camera, final Ride ride,
                                final double from, final double to, final boolean edge, final RidePalette palette) {
    final int first = (int) Math.floor(from / BAND);
    for (int band = (int) Math.ceil(to / BAND) - 1; band >= first; band--) {
      final double start = Math.max(from, band * BAND);
      final BranchKind kind = ride.kind(start);
      graphics.setFill(color(kind, edge, false, palette));
      strip(graphics, camera, ride, start, Math.min(to, (band + 1) * BAND + OVERLAP), half(kind, edge));
    }
  }

  /**
   * Gives the half width of a road of a kind in a layer: its edges or its surface.
   *
   * @param kind the kind of the road.
   * @param edge {@code true} for its edges, {@code false} for its surface.
   *
   * @return the half width, in metres: 0 for the edges of a track, which has none.
   */
  private static double half(final BranchKind kind, final boolean edge) {
    if (!edge) {
      return ROAD;
    }
    return kind == BranchKind.TARMAC ? ROAD + EDGE : 0;
  }

  /**
   * Gives the colour of a road of a kind in a layer.
   *
   * @param kind the kind of the road.
   * @param edge {@code true} for its edges, {@code false} for its surface.
   * @param dark whether its surface is of the darker shade.
   * @param palette the colours.
   *
   * @return white for the edges, grey for a tarmac road, brown for a track.
   */
  private static Color color(final BranchKind kind, final boolean edge, final boolean dark,
                             final RidePalette palette) {
    if (edge) {
      return palette.color(RidePalette.EDGE);
    }
    if (kind == BranchKind.TRACK) {
      return palette.color(dark ? RidePalette.TRACK_DARK : RidePalette.TRACK);
    }
    return palette.color(dark ? RidePalette.ROAD_DARK : RidePalette.ROAD);
  }

  /**
   * Fills a road of the map around a junction, its edges or its surface in the colour of its kind, piece by piece
   * between its points, from its last point to its first; the sides of two pieces meet at their common point, across
   * the heading of the road there.
   *
   * @param graphics the graphics of the canvas.
   * @param camera the camera.
   * @param road the road.
   * @param edge {@code true} for its edges, {@code false} for its surface.
   * @param palette the colours.
   */
  private static void drawMapRoad(final GraphicsContext graphics, final Camera camera, final RideRoad road,
                                  final boolean edge, final RidePalette palette) {
    if (edge) {
      graphics.setStroke(color(road.kind(), false, false, palette));
      graphics.setLineWidth(camera.height() * ROAD_LINE);
      for (int point = road.size() - 2; point >= 0; point--) {
        camera.stroke(graphics, new double[] {road.east(point), road.north(point)},
                      new double[] {road.east(point + 1), road.north(point + 1)});
      }
    }
    graphics.setFill(color(road.kind(), edge, false, palette));
    final double half = half(road.kind(), edge);
    for (int point = road.size() - 2; point >= 0; point--) {
      final double[] near = side(road, point, half);
      final double[] far = side(road, point + 1, half);
      camera.fill(graphics, new double[] {near[0], near[2], far[2], far[0]},
                  new double[] {near[1], near[RIGHT_NORTH], far[RIGHT_NORTH], far[1]});
    }
  }

  /**
   * Fills the dashed middle line of a tarmac road of the map, as the road ridden has: dashes of {@value #DASH} m
   * separated by as much, from its first point; a track has none.
   *
   * @param graphics the graphics of the canvas.
   * @param camera the camera.
   * @param road the road.
   * @param palette the colours.
   */
  private static void drawMapLane(final GraphicsContext graphics, final Camera camera, final RideRoad road,
                                  final RidePalette palette) {
    if (road.kind() != BranchKind.TARMAC) {
      return;
    }
    graphics.setFill(palette.color(RidePalette.LANE));
    double along = 0;
    for (int point = 0; point < road.size() - 1; point++) {
      final double toEast = road.east(point + 1) - road.east(point);
      final double toNorth = road.north(point + 1) - road.north(point);
      final double length = Math.hypot(toEast, toNorth);
      double at = along;
      while (at < along + length) {
        final double next = Math.min(along + length, (Math.floor(at / DASH) + 1) * DASH);
        if ((long) Math.floor(at / DASH) % 2 == 0) {
          dash(graphics, camera, road, point, (at - along) / length, (next - along) / length);
        }
        at = next;
      }
      along += length;
    }
  }

  /**
   * Fills a dash of the middle line of a road of the map, between two shares of one of its pieces.
   *
   * @param graphics the graphics of the canvas, its fill set.
   * @param camera the camera.
   * @param road the road.
   * @param point the index of the first point of the piece.
   * @param from the share of the piece where the dash begins, from 0 to 1.
   * @param to the share where it ends.
   */
  private static void dash(final GraphicsContext graphics, final Camera camera, final RideRoad road, final int point,
                           final double from, final double to) {
    final double toEast = road.east(point + 1) - road.east(point);
    final double toNorth = road.north(point + 1) - road.north(point);
    final double length = Math.hypot(toEast, toNorth);
    final double rightEast = toNorth / length * LANE;
    final double rightNorth = -toEast / length * LANE;
    final double nearEast = road.east(point) + toEast * from;
    final double nearNorth = road.north(point) + toNorth * from;
    final double farEast = road.east(point) + toEast * to;
    final double farNorth = road.north(point) + toNorth * to;
    camera.fill(graphics, new double[] {nearEast - rightEast, nearEast + rightEast, farEast + rightEast,
                                        farEast - rightEast},
                new double[] {nearNorth - rightNorth, nearNorth + rightNorth, farNorth + rightNorth,
                              farNorth - rightNorth});
  }

  /**
   * Tells whether a road of the map is in sight: a point of it within {@value #AHEAD} m of the motorbike, the distance
   * of the road drawn ahead.
   *
   * @param road the road of the map.
   * @param rider the motorbike.
   *
   * @return {@code true} if it is drawn.
   */
  static boolean inSight(final RideRoad road, final Rider rider) {
    for (int point = 0; point < road.size(); point++) {
      if (Math.hypot(road.east(point) - rider.east(), road.north(point) - rider.north()) <= AHEAD) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gives the points either side of a road of the map at one of its points.
   *
   * @param road the road.
   * @param point the index of the point.
   * @param half the distance from the middle, in metres.
   *
   * @return the metres east and north of the point at the left, then of the point at the right.
   */
  private static double[] side(final RideRoad road, final int point, final double half) {
    final double heading = Math.toRadians(road.heading(point));
    final double rightEast = Math.cos(heading) * half;
    final double rightNorth = -Math.sin(heading) * half;
    return new double[] {road.east(point) - rightEast, road.north(point) - rightNorth, road.east(point) + rightEast,
                         road.north(point) + rightNorth};
  }

  /**
   * Fills the branches of a junction but its exit, each from the junction or from the ring of a roundabout, its
   * edges or its surface in the colour of its kind; the road beyond the junction is drawn instead of the exit.
   *
   * @param graphics the graphics of the canvas.
   * @param camera the camera.
   * @param junction the junction.
   * @param edge {@code true} for the edges, {@code false} for the surfaces.
   * @param palette the colours.
   */
  private static void drawBranches(final GraphicsContext graphics, final Camera camera, final RideJunction junction,
                                   final boolean edge, final RidePalette palette) {
    final double[] centre = {junction.ring().map(RideJunction.Ring::east).orElse(junction.east()),
                             junction.ring().map(RideJunction.Ring::north).orElse(junction.north())};
    final double start = junction.ring().map(RideJunction.Ring::radius).orElse(0.0);
    for (int branch = 0; branch < junction.tulip().branches().size(); branch++) {
      if (!junction.isRight(branch)) {
        final double heading = Math.toRadians(junction.bearing(branch));
        final BranchKind kind = junction.tulip().kinds().get(branch);
        final double half = half(kind, edge);
        graphics.setFill(color(kind, edge, false, palette));
        for (double along = start + BRANCH - BRANCH_STEP; along >= start; along -= BRANCH_STEP) {
          ray(graphics, camera, centre, heading, along, along + BRANCH_STEP, half);
        }
      }
    }
  }

  /**
   * Fills the ring of a roundabout.
   *
   * @param graphics the graphics of the canvas.
   * @param camera the camera.
   * @param junction the junction; nothing is drawn for a crossing.
   * @param palette the colours.
   */
  private static void drawRing(final GraphicsContext graphics, final Camera camera, final RideJunction junction,
                               final RidePalette palette) {
    junction.ring().ifPresent(ring -> {
      graphics.setFill(palette.color(RidePalette.ROAD));
      for (int segment = 0; segment < RING_SEGMENTS; segment++) {
        arc(graphics, camera, ring, segment, ring.radius() - ROAD, ring.radius() + ROAD);
      }
    });
  }

  /**
   * Draws the island of a roundabout: its white edge and its grass.
   *
   * @param graphics the graphics of the canvas.
   * @param camera the camera.
   * @param junction the junction.
   * @param palette the colours.
   */
  private static void drawIsland(final GraphicsContext graphics, final Camera camera, final RideJunction junction,
                                 final RidePalette palette) {
    junction.ring().ifPresent(ring -> {
      for (int segment = 0; segment < RING_SEGMENTS; segment++) {
        graphics.setFill(palette.color(RidePalette.EDGE));
        arc(graphics, camera, ring, segment, ring.radius() - ROAD - EDGE, ring.radius() - ROAD);
        graphics.setFill(palette.color(RidePalette.GRASS_DARK));
        arc(graphics, camera, ring, segment, 0, ring.radius() - ROAD - EDGE);
      }
    });
  }

  /**
   * Fills a piece of a branch: a rectangle along a heading from a centre, between two distances.
   *
   * @param graphics the graphics of the canvas.
   * @param camera the camera.
   * @param centre the metres east and north of the centre.
   * @param heading the heading of the branch, in radians.
   * @param from the distance of the near end from the centre.
   * @param to the distance of the far end.
   * @param half the half width, in metres.
   */
  private static void ray(final GraphicsContext graphics, final Camera camera, final double[] centre,
                          final double heading, final double from, final double to, final double half) {
    final double forwardEast = Math.sin(heading);
    final double forwardNorth = Math.cos(heading);
    final double rightEast = Math.cos(heading) * half;
    final double rightNorth = -Math.sin(heading) * half;
    camera.fill(graphics, new double[] {centre[0] + forwardEast * from - rightEast,
                                        centre[0] + forwardEast * from + rightEast,
                                        centre[0] + forwardEast * to + rightEast,
                                        centre[0] + forwardEast * to - rightEast},
                new double[] {centre[1] + forwardNorth * from - rightNorth,
                              centre[1] + forwardNorth * from + rightNorth,
                              centre[1] + forwardNorth * to + rightNorth,
                              centre[1] + forwardNorth * to - rightNorth});
  }

  /**
   * Fills a piece of a ring: between two radii and two angles.
   *
   * @param graphics the graphics of the canvas.
   * @param camera the camera.
   * @param ring the ring.
   * @param segment the index of the piece, of {@value #RING_SEGMENTS}.
   * @param inner the inner radius, in metres.
   * @param outer the outer radius, in metres.
   */
  private static void arc(final GraphicsContext graphics, final Camera camera, final RideJunction.Ring ring,
                          final int segment, final double inner, final double outer) {
    final double first = Math.toRadians(FULL_TURN * segment / RING_SEGMENTS);
    final double second = Math.toRadians(FULL_TURN * (segment + 1) / RING_SEGMENTS);
    camera.fill(graphics, new double[] {ring.east() + inner * Math.sin(first), ring.east() + outer * Math.sin(first),
                                        ring.east() + outer * Math.sin(second), ring.east() + inner * Math.sin(second)},
                new double[] {ring.north() + inner * Math.cos(first), ring.north() + outer * Math.cos(first),
                              ring.north() + outer * Math.cos(second), ring.north() + inner * Math.cos(second)});
  }

  /**
   * Draws an obstacle on the road in relief (asked by the maintainer on 2026-11-22: more than a grey plate): a bump, a
   * mound across the road {@value #BUMP_HEIGHT} m high, its front in three shades, the darkest at its foot, the
   * lightest at its crest, its shadow on the road beyond it; a hole, wider than long, its broken edge, its dark
   * opening and its far wall seen inside, {@value #HOLE_DEPTH} m deep, clipped by the opening.
   * An obstacle partly behind the camera is not drawn: it has passed under the motorbike.
   *
   * @param graphics the graphics of the canvas.
   * @param camera the camera.
   * @param ride the road.
   * @param item the obstacle.
   * @param palette the colours.
   */
  private static void drawObstacle(final GraphicsContext graphics, final Camera camera, final Ride ride,
                                   final RideItem item, final RidePalette palette) {
    if (item.kind() == RideItem.Kind.BUMP) {
      final double near = item.distance() - BUMP / 2;
      final double crest = item.distance();
      graphics.setFill(palette.color(RidePalette.BUMP_DARK));
      slope(graphics, camera, ride, crest, 0, crest + BUMP / 2 + BUMP_SHADOW, 0);
      final String[] shades = {RidePalette.BUMP_DARK, RidePalette.BUMP, RidePalette.EDGE};
      for (int shade = 0; shade < shades.length; shade++) {
        final double from = (double) shade / shades.length;
        final double to = (double) (shade + 1) / shades.length;
        graphics.setFill(palette.color(shades[shade]));
        slope(graphics, camera, ride, near + from * BUMP / 2, from * BUMP_HEIGHT, near + to * BUMP / 2,
              to * BUMP_HEIGHT);
      }
      return;
    }
    final double heading = Math.toRadians(ride.heading(item.distance()));
    graphics.setFill(palette.color(RidePalette.BUMP));
    hole(graphics, camera, item, heading, HOLE * HOLE_RIM, false);
    graphics.setFill(palette.color(RidePalette.HOLE));
    hole(graphics, camera, item, heading, HOLE, false);
    graphics.save();
    graphics.clip();
    graphics.setFill(palette.color(RidePalette.HOLE_WALL));
    hole(graphics, camera, item, heading, HOLE, true);
    graphics.restore();
  }

  /**
   * Fills a slope across the road between two distances, each at a height.
   *
   * @param graphics the graphics of the canvas.
   * @param camera the camera.
   * @param ride the road.
   * @param from the distance of its near edge.
   * @param fromHeight the height of its near edge, in metres.
   * @param to the distance of its far edge.
   * @param toHeight the height of its far edge, in metres.
   */
  private static void slope(final GraphicsContext graphics, final Camera camera, final Ride ride, final double from,
                            final double fromHeight, final double to, final double toHeight) {
    final double[] near = side(ride, from, ROAD);
    final double[] far = side(ride, to, ROAD);
    camera.fillRaised(graphics, new double[] {near[0], near[2], far[2], far[0]},
                      new double[] {near[1], near[RIGHT_NORTH], far[RIGHT_NORTH], far[1]},
                      new double[] {fromHeight, fromHeight, toHeight, toHeight});
  }

  /**
   * Fills the opening of a hole, a polygon of {@value #HOLE_SIDES} sides on the ground, or its far wall, the far half
   * of its edge and the same {@value #HOLE_DEPTH} m lower.
   *
   * @param graphics the graphics of the canvas.
   * @param camera the camera.
   * @param item the hole.
   * @param heading the heading of the road at the hole, in radians.
   * @param radius the radius of the polygon, in metres.
   * @param wall {@code true} for the far wall, {@code false} for the opening.
   */
  private static void hole(final GraphicsContext graphics, final Camera camera, final RideItem item,
                           final double heading, final double radius, final boolean wall) {
    final int corners = wall ? HOLE_SIDES + 2 : HOLE_SIDES;
    final double[] easts = new double[corners];
    final double[] norths = new double[corners];
    final double[] heights = new double[corners];
    for (int corner = 0; corner < corners; corner++) {
      final int around = wall ? Math.min(corner, corners - 1 - corner) : corner;
      final double angle = wall ? Math.PI * around / (HOLE_SIDES / 2.0) - Math.PI / 2 : 2 * Math.PI * corner / corners;
      final double ahead = Math.cos(angle) * radius;
      final double right = Math.sin(angle) * radius * HOLE_WIDE;
      easts[corner] = item.east() + Math.sin(heading) * ahead + Math.cos(heading) * right;
      norths[corner] = item.north() + Math.cos(heading) * ahead - Math.sin(heading) * right;
      heights[corner] = wall && corner > around ? -HOLE_DEPTH : 0;
    }
    camera.fillRaised(graphics, easts, norths, heights);
  }

  /**
   * Places the trees not noted along the road, at the roadside, not all the time: every {@value #TREE_BANDS} bands, a
   * draw from the index of the band (the same tree at each frame) puts one at the left, one at the right or none,
   * none within {@value #TREE_CLEAR} m of a junction.
   *
   * @param camera the camera.
   * @param ride the road.
   * @param from the distance where the road drawn begins.
   * @param to the distance where it ends.
   *
   * @return the trees, as billboards.
   */
  static List<Upright> trees(final Camera camera, final Ride ride, final double from, final double to) {
    final List<Upright> trees = new ArrayList<>();
    for (int band = (int) Math.ceil(from / (BAND * TREE_BANDS)); band * BAND * TREE_BANDS <= to; band++) {
      final double along = band * BAND * TREE_BANDS;
      final int draw = Math.floorMod(band * TREE_MIX, TREE_DRAWS);
      final boolean clear = ride.junctions().stream()
                                .allMatch(junction -> Math.abs(junction.distance() - along) > TREE_CLEAR);
      if (draw < 2 && clear) {
        final double[] sides = side(ride, along, TREE_SIDE);
        trees.add(Billboard.of(camera, RideItem.Kind.TREE, sides[2 * draw], sides[2 * draw + 1]));
      }
    }
    return trees;
  }

  /**
   * Draws the finish line across the end of the road: two rows of squares.
   *
   * @param graphics the graphics of the canvas.
   * @param camera the camera.
   * @param ride the road.
   * @param palette the colours.
   */
  private static void drawFinish(final GraphicsContext graphics, final Camera camera, final Ride ride,
                                 final RidePalette palette) {
    for (int row = 0; row < 2; row++) {
      final double[] near = side(ride, ride.length() - (row + 1) * FINISH, 1);
      final double[] far = side(ride, ride.length() - row * FINISH, 1);
      for (double across = -ROAD; across < ROAD; across += FINISH) {
        final boolean light = Math.floorMod((int) Math.round(across / FINISH) + row, 2) == 0;
        graphics.setFill(palette.color(light ? RidePalette.EDGE : RidePalette.INK));
        camera.fill(graphics, new double[] {across(near, 0, across), across(near, 0, across + FINISH),
                                            across(far, 0, across + FINISH), across(far, 0, across)},
                    new double[] {across(near, 1, across), across(near, 1, across + FINISH),
                                  across(far, 1, across + FINISH), across(far, 1, across)});
      }
    }
  }

  /**
   * Gives a coordinate of a point across the road, between the points a metre either side of its middle.
   *
   * @param sides the points a metre at the left and at the right of the middle ({@link #side}).
   * @param coordinate 0 for the metres east, 1 for the metres north.
   * @param across the distance from the middle, to the right, in metres.
   *
   * @return the coordinate of the point.
   */
  private static double across(final double[] sides, final int coordinate, final double across) {
    final double middle = (sides[coordinate] + sides[2 + coordinate]) / 2;
    return middle + (sides[2 + coordinate] - middle) * across;
  }

  /**
   * Draws the speedometer at the bottom left of the canvas, an instrument of the motorbike in 8 bits (asked by the
   * maintainer on 2026-11-22: the speed and the trip in the game): a gauge of {@value #GAUGE_BLOCKS} blocks rising
   * from 0 to {@link Rider#MAX_SPEED}, lit up to the speed, red above the limit of the speed zone, the block of the
   * limit marked in orange beneath; under it the speed in figures of pixels, the unlit figures showing as on a
   * display of segments, and its unit.
   *
   * @param graphics the graphics of the canvas.
   * @param height the height of the canvas.
   * @param unit the side of a pixel of the instruments, in pixels of the canvas.
   * @param rider the motorbike.
   * @param dash the texts of the instruments.
   * @param palette the colours.
   */
  private static void drawSpeedometer(final GraphicsContext graphics, final double height, final double unit,
                                      final Rider rider, final Dash dash, final RidePalette palette) {
    final int gauge = GAUGE_BLOCKS * (GAUGE_BLOCK + 1) - 1;
    final int tallest = GAUGE_LOW + (GAUGE_BLOCKS - 1) / 2;
    final int inner = DASH_PADDING + tallest + DASH_PADDING + FIGURE_HEIGHT;
    final double left = DASH_MARGIN * unit;
    final double top = height - (DASH_MARGIN + DASH_PADDING * 2 + inner) * unit;
    panel(graphics, new Rectangle2D(left, top, (gauge + DASH_PADDING * 2) * unit, (inner + DASH_PADDING * 2) * unit),
          RidePalette.INK, unit, palette);
    final double limit = rider.ride().zone(rider.progress()).map(zone -> (double) zone.speed())
                              .orElse(Rider.MAX_SPEED + 1);
    final double bottom = top + (DASH_PADDING + tallest) * unit;
    for (int block = 0; block < GAUGE_BLOCKS; block++) {
      final double from = Rider.MAX_SPEED * block / GAUGE_BLOCKS;
      final double to = Rider.MAX_SPEED * (block + 1) / GAUGE_BLOCKS;
      final String lit = from >= limit ? RidePalette.RED : RidePalette.WHITE;
      final String token = rider.speed() <= from ? RidePalette.UNLIT : lit;
      final double x = left + (DASH_PADDING + block * (GAUGE_BLOCK + 1)) * unit;
      final int rise = GAUGE_LOW + block / 2;
      graphics.setFill(palette.color(token));
      graphics.fillRect(x, bottom - rise * unit, GAUGE_BLOCK * unit, rise * unit);
      if (limit > from && limit <= to) {
        graphics.setFill(palette.color(RidePalette.ORANGE));
        graphics.fillRect(x, bottom + unit / 2, GAUGE_BLOCK * unit, unit);
      }
    }
    final String figures = String.format(Locale.ROOT, "%" + SPEED_FIGURES + "d", Math.round(rider.speed()));
    final double line = bottom + DASH_PADDING * unit;
    for (int i = 0; i < SPEED_FIGURES; i++) {
      final double x = left + (DASH_PADDING + i * (FIGURE_WIDTH + 1)) * unit;
      drawFigure(graphics, FIGURES.get(GHOST), x, line, unit, palette.color(RidePalette.UNLIT));
      if (figures.charAt(i) != ' ') {
        drawFigure(graphics, FIGURES.get(figures.charAt(i) - '0'), x, line, unit, palette.color(RidePalette.WHITE));
      }
    }
    unit(graphics, dash.speedUnit(), left + (DASH_PADDING + SPEED_FIGURES * (FIGURE_WIDTH + 1)) * unit,
         line + FIGURE_HEIGHT * unit, unit, palette);
  }

  /**
   * Draws the trip meter at the bottom right of the canvas, as the trip meter of a rally: a case, the unit above, the
   * kilometres in black cells and the hundredths in red ones, as the rolls of a counter, the separator of the
   * decimals of the language between them; it turns over after 999.99 km.
   *
   * @param graphics the graphics of the canvas.
   * @param width the width of the canvas.
   * @param height the height of the canvas.
   * @param unit the side of a pixel of the instruments, in pixels of the canvas.
   * @param trip the trip, in metres, 0 or more.
   * @param dash the texts of the instruments.
   * @param palette the colours.
   */
  private static void drawTripMeter(final GraphicsContext graphics, final double width, final double height,
                                    final double unit, final double trip, final Dash dash,
                                    final RidePalette palette) {
    final int cell = FIGURE_WIDTH + 2;
    final int cells = TRIP_FIGURES + TRIP_DECIMALS;
    final int inner = cells * (cell + 1) + 1;
    final int tall = UNIT_SIZE + 1 + FIGURE_HEIGHT + 2;
    final double right = width - DASH_MARGIN * unit;
    final double left = right - (inner + DASH_PADDING * 2) * unit;
    final double top = height - (DASH_MARGIN + DASH_PADDING * 2 + tall) * unit;
    panel(graphics, new Rectangle2D(left, top, right - left, (tall + DASH_PADDING * 2) * unit), RidePalette.GREY_DARK,
          unit, palette);
    graphics.setTextAlign(TextAlignment.RIGHT);
    unit(graphics, dash.tripUnit(), right - DASH_PADDING * unit, top + (DASH_PADDING + UNIT_SIZE) * unit, unit,
         palette);
    graphics.setTextAlign(TextAlignment.LEFT);
    final long hundredths = Math.round(trip / METRES_PER_HUNDREDTH) % TURN_OVER;
    final String figures = String.format(Locale.ROOT, TRIP_FORMAT, hundredths);
    final double row = top + (DASH_PADDING + UNIT_SIZE + 1) * unit;
    for (int i = 0; i < cells; i++) {
      final double x = left + (DASH_PADDING + i * (cell + 1) + (i >= TRIP_FIGURES ? 1 : 0)) * unit;
      graphics.setFill(palette.color(i >= TRIP_FIGURES ? RidePalette.RED : RidePalette.INK));
      graphics.fillRect(x, row, cell * unit, (FIGURE_HEIGHT + 2) * unit);
      drawFigure(graphics, FIGURES.get(figures.charAt(i) - '0'), x + unit, row + unit, unit,
                 palette.color(RidePalette.WHITE));
    }
    final double separator = left + (DASH_PADDING + TRIP_FIGURES * (cell + 1)) * unit;
    graphics.setFill(palette.color(RidePalette.WHITE));
    graphics.fillRect(separator - unit / 2, row + FIGURE_HEIGHT * unit, unit, unit);
    if (dash.separator() == ',') {
      graphics.fillRect(separator - unit, row + (FIGURE_HEIGHT + 1) * unit, unit, unit);
    }
  }

  /**
   * Draws the case of an instrument: its background in a frame of grey.
   *
   * @param graphics the graphics of the canvas.
   * @param bounds its left, its top, its width and its height, in pixels of the canvas.
   * @param background the token of its background.
   * @param frame the width of the frame, in pixels of the canvas: a pixel of the instruments.
   * @param palette the colours.
   */
  private static void panel(final GraphicsContext graphics, final Rectangle2D bounds, final String background,
                            final double frame, final RidePalette palette) {
    graphics.setFill(palette.color(RidePalette.GREY));
    graphics.fillRect(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
    graphics.setFill(palette.color(background));
    graphics.fillRect(bounds.getMinX() + frame, bounds.getMinY() + frame, bounds.getWidth() - 2 * frame,
                      bounds.getHeight() - 2 * frame);
  }

  /**
   * Writes the unit of an instrument, its baseline at a point, in the alignment set.
   *
   * @param graphics the graphics of the canvas.
   * @param text the unit, from the bundle.
   * @param x the place of the text, in pixels of the canvas.
   * @param baseline its baseline.
   * @param unit the side of a pixel of the instruments.
   * @param palette the colours.
   */
  private static void unit(final GraphicsContext graphics, final String text, final double x, final double baseline,
                           final double unit, final RidePalette palette) {
    graphics.setFill(palette.color(RidePalette.WHITE));
    graphics.setFont(Font.font("System", FontWeight.BOLD, UNIT_SIZE * unit));
    graphics.setTextBaseline(VPos.BASELINE);
    graphics.fillText(text, x, baseline);
  }

  /**
   * Draws a figure of pixels, its top left at a point.
   *
   * @param graphics the graphics of the canvas.
   * @param figure the rows of its pixels, each X lit.
   * @param x its left, in pixels of the canvas.
   * @param y its top.
   * @param unit the side of a pixel.
   * @param color the colour of its pixels lit.
   */
  private static void drawFigure(final GraphicsContext graphics, final List<String> figure, final double x,
                                 final double y, final double unit, final Color color) {
    graphics.setFill(color);
    for (int row = 0; row < figure.size(); row++) {
      for (int column = 0; column < figure.get(row).length(); column++) {
        if (figure.get(row).charAt(column) == 'X') {
          graphics.fillRect(x + column * unit, y + row * unit, unit, unit);
        }
      }
    }
  }

  /**
   * Draws a sprite of pixels, its bottom middle at a point, turned around it.
   *
   * @param graphics the graphics of the canvas.
   * @param sprite the rows of its pixels, the same length, each letter a colour of the palette, a dot transparent.
   * @param x the middle of its bottom, in pixels of the canvas.
   * @param y its bottom, in pixels of the canvas.
   * @param pixel the side of a pixel of the sprite, in pixels of the canvas.
   * @param lean the angle it is turned by, in degrees, clockwise.
   * @param palette the colours.
   */
  static void drawSprite(final GraphicsContext graphics, final List<String> sprite, final double x, final double y,
                         final double pixel, final double lean, final RidePalette palette) {
    final int columns = sprite.getFirst().length();
    graphics.save();
    graphics.translate(x, y);
    graphics.rotate(lean);
    for (int row = 0; row < sprite.size(); row++) {
      for (int column = 0; column < columns; column++) {
        final Color color = palette.sprite(sprite.get(row).charAt(column));
        if (color != null) {
          graphics.setFill(color);
          graphics.fillRect((column - columns / 2.0) * pixel, (row - sprite.size()) * pixel, pixel + SEAM,
                            pixel + SEAM);
        }
      }
    }
    graphics.restore();
  }

  // *******************************************************************************************************************
  // Inner types
  // *******************************************************************************************************************

  /**
   * Something standing beside the road, drawn upright, the farthest first.
   */
  sealed interface Upright permits Billboard, Beacon, Warning {

    /**
     * Gives its distance ahead of the camera.
     *
     * @return the distance, in metres.
     */
    double ahead();

    /**
     * Draws it, its foot on the ground, as large as its metres seen from there.
     *
     * @param graphics the graphics of the canvas.
     * @param camera the camera.
     * @param palette the colours.
     */
    void draw(GraphicsContext graphics, Camera camera, RidePalette palette);

  }

  /**
   * The beacon of a waypoint at the roadside (asked by the maintainer on 2026-11-21: the waypoints drawn in the scene):
   * a post and a disc in the colours of the waypoint in the roadbook, its letters on it.
   *
   * @param kind the kind of the waypoint, marked in the roadbook.
   * @param east the metres east of its foot.
   * @param north the metres north of its foot.
   * @param ahead its distance ahead of the camera, in metres.
   */
  record Beacon(WaypointKind kind, double east, double north, double ahead) implements Upright {

    /**
     * {@inheritDoc}
     */
    @Override
    public void draw(final GraphicsContext graphics, final Camera camera, final RidePalette palette) {
      final double[] foot = camera.screen(ahead, camera.view(east, north)[1]);
      final double scale = camera.focal() / ahead;
      final double disc = BEACON_DISC * scale;
      final double top = foot[1] - BEACON_HEIGHT * scale;
      graphics.setFill(palette.color(RidePalette.INK));
      graphics.fillRect(foot[0] - disc * BEACON_POST / 2, top, disc * BEACON_POST, BEACON_HEIGHT * scale);
      graphics.fillOval(foot[0] - disc / 2 - disc * BEACON_RING, top - disc / 2 - disc * BEACON_RING,
                        disc + 2 * disc * BEACON_RING, disc + 2 * disc * BEACON_RING);
      graphics.setFill(palette.color(RidePalette.waypoint(kind)));
      graphics.fillOval(foot[0] - disc / 2, top - disc / 2, disc, disc);
      graphics.setFill(palette.color(RidePalette.INK));
      graphics.setFont(Font.font("System", FontWeight.BOLD, disc * BEACON_TEXT / Math.max(2, kind.mark().length())));
      graphics.setTextAlign(TextAlignment.CENTER);
      graphics.setTextBaseline(VPos.CENTER);
      graphics.fillText(kind.mark(), foot[0], top);
    }

  }

  /**
   * The warning sign of an obstacle, {@value #SIGN_AHEAD} m before it at the right of the road (asked by the maintainer
   * on 2026-11-22): a post and a red triangle, white inside, a bump drawn as a half disc on a line, a hole as a half
   * disc under it.
   *
   * @param kind the obstacle: a bump or a hole.
   * @param east the metres east of its foot.
   * @param north the metres north of its foot.
   * @param ahead its distance ahead of the camera, in metres.
   */
  record Warning(RideItem.Kind kind, double east, double north, double ahead) implements Upright {

    /**
     * {@inheritDoc}
     */
    @Override
    public void draw(final GraphicsContext graphics, final Camera camera, final RidePalette palette) {
      final double[] foot = camera.screen(ahead, camera.view(east, north)[1]);
      final double scale = camera.focal() / ahead;
      final double side = SIGN_SIDE * scale;
      final double tall = side * TRIANGLE;
      final double bottom = foot[1] - SIGN_HEIGHT * scale;
      graphics.setFill(palette.color(RidePalette.GREY));
      graphics.fillRect(foot[0] - side * BEACON_POST / 2, bottom, side * BEACON_POST, SIGN_HEIGHT * scale);
      graphics.setFill(palette.color(RidePalette.RED));
      graphics.fillPolygon(new double[] {foot[0] - side / 2, foot[0] + side / 2, foot[0]},
                           new double[] {bottom, bottom, bottom - tall}, 2 + 1);
      final double border = tall * SIGN_BORDER;
      final double inner = (tall - border * 2 - border) / TRIANGLE;
      graphics.setFill(palette.color(RidePalette.WHITE));
      graphics.fillPolygon(new double[] {foot[0] - inner / 2, foot[0] + inner / 2, foot[0]},
                           new double[] {bottom - border, bottom - border, bottom - border - inner * TRIANGLE},
                           2 + 1);
      final double width = side * SIGN_SYMBOL;
      final double line = bottom - border * 2;
      graphics.setFill(palette.color(RidePalette.INK));
      graphics.fillRect(foot[0] - width / 2, line - width * BEACON_POST / 2, width, width * BEACON_POST);
      final boolean bump = kind == RideItem.Kind.BUMP;
      graphics.fillArc(foot[0] - width / 2 * (1 - SIGN_BORDER), line - border, width * (1 - SIGN_BORDER), border * 2,
                       bump ? 0 : HALF_TURN, HALF_TURN, ArcType.CHORD);
    }

  }

  /**
   * An element standing beside the road, drawn upright as a sprite of pixels scaled by its distance.
   *
   * @param kind what it is: a tree, a house or a castle.
   * @param east the metres east of its foot.
   * @param north the metres north of its foot.
   * @param ahead its distance ahead of the camera, in metres: the farthest drawn first.
   */
  record Billboard(RideItem.Kind kind, double east, double north, double ahead) implements Upright {

    /**
     * Places an element seen from the camera.
     *
     * @param camera the camera.
     * @param kind what it is.
     * @param east the metres east of its foot.
     * @param north the metres north of its foot.
     *
     * @return the billboard.
     */
    static Billboard of(final Camera camera, final RideItem.Kind kind, final double east, final double north) {
      return new Billboard(kind, east, north, camera.view(east, north)[0]);
    }

    /**
     * Draws the element, its foot on the ground, as large as its metres seen from there.
     *
     * @param graphics the graphics of the canvas.
     * @param camera the camera.
     * @param palette the colours.
     */
    @Override
    public void draw(final GraphicsContext graphics, final Camera camera, final RidePalette palette) {
      final double[] foot = camera.screen(ahead, camera.view(east, north)[1]);
      final double scale = camera.focal() / ahead;
      switch (kind) {
        case HOUSE -> drawSprite(graphics, HOUSE, foot[0], foot[1], HOUSE_PIXEL * scale, 0, palette);
        case CASTLE -> drawSprite(graphics, CASTLE, foot[0], foot[1], CASTLE_PIXEL * scale, 0, palette);
        default -> drawSprite(graphics, TREE, foot[0], foot[1], TREE_PIXEL * scale, 0, palette);
      }
    }

  }

  /**
   * The texts of the instruments, from the bundle and the language of the application.
   *
   * @param speedUnit the unit of the speed.
   * @param tripUnit the unit of the trip.
   * @param separator the separator of the decimals of the language.
   */
  record Dash(String speedUnit, String tripUnit, char separator) {
  }

  /**
   * The camera behind the motorbike: its place, its heading and the projection of the ground on the canvas.
   *
   * @param east the metres east of the start of the camera.
   * @param north the metres north of the start of the camera.
   * @param heading its heading, in degrees clockwise from the north.
   * @param width the width of the canvas, in pixels.
   * @param height the height of the canvas, in pixels.
   */
  record Camera(double east, double north, double heading, double width, double height) {

    /**
     * Places the camera {@value #BEHIND} m behind a motorbike, along its heading.
     *
     * @param rider the motorbike.
     * @param width the width of the canvas.
     * @param height the height of the canvas.
     *
     * @return the camera.
     */
    static Camera of(final Rider rider, final double width, final double height) {
      final double heading = Math.toRadians(rider.heading());
      return new Camera(rider.east() - Math.sin(heading) * BEHIND, rider.north() - Math.cos(heading) * BEHIND,
                        rider.heading(), width, height);
    }

    /**
     * Gives the height of the horizon on the canvas.
     *
     * @return the horizon, in pixels from the top.
     */
    double horizon() {
      return height * HORIZON;
    }

    /**
     * Gives the focal length.
     *
     * @return the focal length, in pixels.
     */
    double focal() {
      return Math.min(width, height * WIDEST) * FOCAL;
    }

    /**
     * Gives the distance of a point of the ground ahead of the camera and to its right.
     *
     * @param pointEast the metres east of the point.
     * @param pointNorth the metres north of the point.
     *
     * @return its distance ahead, then to the right, in metres.
     */
    double[] view(final double pointEast, final double pointNorth) {
      final double radians = Math.toRadians(heading);
      final double toEast = pointEast - east;
      final double toNorth = pointNorth - north;
      return new double[] {toEast * Math.sin(radians) + toNorth * Math.cos(radians),
                           toEast * Math.cos(radians) - toNorth * Math.sin(radians)};
    }

    /**
     * Places on the canvas a point of the ground seen ahead of the camera.
     *
     * @param ahead its distance ahead, in metres, {@value #NEAR} at least.
     * @param right its distance to the right, in metres.
     *
     * @return its place, in pixels.
     */
    double[] screen(final double ahead, final double right) {
      return new double[] {width / 2 + right * focal() / ahead, horizon() + EYE * focal() / ahead};
    }

    /**
     * Fills a polygon of the ground, cut where it passes behind the camera.
     *
     * @param graphics the graphics of the canvas.
     * @param easts the metres east of its corners.
     * @param norths the metres north of its corners.
     */
    void fill(final GraphicsContext graphics, final double[] easts, final double[] norths) {
      final List<double[]> cut = new ArrayList<>();
      for (int i = 0; i < easts.length; i++) {
        final double[] current = view(easts[i], norths[i]);
        final double[] next = view(easts[(i + 1) % easts.length], norths[(i + 1) % easts.length]);
        if (current[0] >= NEAR) {
          cut.add(current);
        }
        if (current[0] >= NEAR != next[0] >= NEAR) {
          final double share = (NEAR - current[0]) / (next[0] - current[0]);
          cut.add(new double[] {NEAR, current[1] + share * (next[1] - current[1])});
        }
      }
      if (cut.size() > 2) {
        final double[] xs = new double[cut.size()];
        final double[] ys = new double[cut.size()];
        for (int i = 0; i < cut.size(); i++) {
          final double[] place = screen(cut.get(i)[0], cut.get(i)[1]);
          xs[i] = place[0];
          ys[i] = place[1];
        }
        graphics.fillPolygon(xs, ys, cut.size());
      }
    }

    /**
     * Draws a line on the ground between two points, cut where it passes behind the camera.
     *
     * @param graphics the graphics of the canvas, its stroke set.
     * @param from the metres east and north of its first point.
     * @param to the metres east and north of its second point; nothing is drawn if both are behind the camera.
     */
    void stroke(final GraphicsContext graphics, final double[] from, final double[] to) {
      final double[] first = view(from[0], from[1]);
      final double[] second = view(to[0], to[1]);
      if (first[0] < NEAR && second[0] < NEAR) {
        return;
      }
      final double[] start = screen(cut(first, second));
      final double[] end = screen(cut(second, first));
      graphics.strokeLine(start[0], start[1], end[0], end[1]);
    }

    /**
     * Places on the canvas a point of the ground seen ahead of the camera.
     *
     * @param seen its distance ahead, {@value #NEAR} m at least, and to the right, in metres.
     *
     * @return its place, in pixels.
     */
    double[] screen(final double[] seen) {
      return screen(seen[0], seen[1]);
    }

    /**
     * Cuts a line where it passes behind the camera: a point behind is moved along the line to {@value #NEAR} m ahead.
     *
     * @param seen the point, ahead and to the right of the camera, in metres.
     * @param other the other end of the line, ahead of the camera if the point is behind it.
     *
     * @return the point, or the point of the line {@value #NEAR} m ahead if it is behind.
     */
    static double[] cut(final double[] seen, final double[] other) {
      if (seen[0] >= NEAR) {
        return seen;
      }
      final double share = (NEAR - seen[0]) / (other[0] - seen[0]);
      return new double[] {NEAR, seen[1] + share * (other[1] - seen[1])};
    }

    /**
     * Fills a polygon whose corners stand above or below the ground, unless a corner is nearer than {@value #NEAR} m
     * ahead of the camera: a raised polygon is small, and never cut. The polygon is left as the path of the graphics,
     * to clip what follows; an empty path if it is not drawn.
     *
     * @param graphics the graphics of the canvas.
     * @param easts the metres east of its corners.
     * @param norths the metres north of its corners.
     * @param heights the heights of its corners above the ground, in metres, negative below it.
     */
    void fillRaised(final GraphicsContext graphics, final double[] easts, final double[] norths,
                    final double[] heights) {
      final double[] xs = new double[easts.length];
      final double[] ys = new double[easts.length];
      for (int i = 0; i < easts.length; i++) {
        final double[] seen = view(easts[i], norths[i]);
        if (seen[0] < NEAR) {
          graphics.beginPath();
          return;
        }
        xs[i] = width / 2 + seen[1] * focal() / seen[0];
        ys[i] = horizon() + (EYE - heights[i]) * focal() / seen[0];
      }
      graphics.beginPath();
      graphics.moveTo(xs[0], ys[0]);
      for (int i = 1; i < easts.length; i++) {
        graphics.lineTo(xs[i], ys[i]);
      }
      graphics.closePath();
      graphics.fill();
    }

  }

}
