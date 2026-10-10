# Tick Benchmark

Performance changes (stat caching, cable snapshots, sleeping machines) are judged by server milliseconds per tick on one fixed scene, measured before and after the change.

## Running it

```sh
./gradlew runTickBenchmark
```

Use `.\gradlew.bat` on Windows. The run is a GameTest server with its own run directory (`run/tickBenchmark` in the main checkout), so it does not share files with `gameTestServer`. It builds the scene, lets it settle, times the server tick, prints the result and exits. The result is also written to `build/tick-benchmark/result.json`.

| Property | Default | Meaning |
|---|---|---|
| `-PbenchmarkSettle` | 100 | Ticks to run before warm-up, so cable caches and panel scans build. |
| `-PbenchmarkWarmup` | 200 | Ticks run and discarded before measuring. |
| `-PbenchmarkTicks` | 400 | Ticks measured. |
| `-PbenchmarkJfr` | off | Also records `build/tick-benchmark/benchmark.jfr` (Java Flight Recorder, `profile` settings). |

A run takes a few minutes while the tick is slow. Use small tick counts for a smoke test, for example `-PbenchmarkSettle=20 -PbenchmarkWarmup=20 -PbenchmarkTicks=60`.

## What is measured

The scene is built from constants in `src/benchmark/java/com/rngtech/benchmark/BenchmarkScene.java`, so every run builds the same world: a cable grid with machines of every registered type and Debug Batteries on Universal Connectors, a third of the machines being Crushers that are refilled and emptied every second, and a tiled solar array of controllers and panels on the same network. Time is fixed at noon with clear weather. `result.json` lists the counts for the scene (cables, connectors, machines, network endpoints) and the items produced; check them first, because a run whose network has no endpoints or whose Crushers produced nothing is not measuring the same thing.

Each measured tick is the time between the start and end of the server tick, taken from the NeoForge tick events. The report gives mean, median, p95, p99 and max in milliseconds, and the garbage-collection time over the window. Compare the median and p95: the mean follows GC pauses. Run the benchmark twice on an idle machine and treat differences smaller than the gap between those two runs as noise.

When the scene changes (new machine types are picked up automatically, but layout constants are not), record a new baseline in the pull request.

## Finding where the time goes

Run with `-PbenchmarkJfr` and open `build/tick-benchmark/benchmark.jfr` in JDK Mission Control, or list the hottest frames from the command line:

```sh
jfr print --events jdk.ExecutionSample build/tick-benchmark/benchmark.jfr
```

To profile a normal world instead, install [spark](https://spark.lucko.me/) on a dev server or client and run `/spark profiler start`, then `/spark profiler stop`; `/spark tps` reports the same ms/tick on a live world. Spark is not a dependency of this project.

## Not covered

The benchmark does not cover fluids, items moving through cables, Forestry carts, chunk loading and unloading, or players. Add them to the scene when a change targets them.
