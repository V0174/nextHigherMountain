# Next Higher Mountain

This java application computes a list of "next higher" mountains from the provided locations and displays them in a chart.

## What is a next higher mountain?

From each starting place:
 
1. Find the nearest peak (mountain, hill, etc.).
2. Find the next nearest peak, which is higher than the previous.
3. Repeat step 2 until the highest peak in the library is found.
4. Display all in a nice chart.

## How to run it?

1. Create a GeoDesk feature library as described [here](https://docs.geodesk.com/tutorial). I'd suggest to start with something smaller than the world or a whole continent. Unless it's Antarctica.
2. Run the application and select the .gol file created.
3. Optionally adapt the query.
4. After clicking the "Spusť" button, the graphs will be computed and displayed.

How to do queries is described in the [GeoDesk pages](https://docs.geodesk.com/tutorial).

If you don't know Java, I would recommend to open this project in a smart Java editor, e.g. [IntelliJ Idea](https://www.jetbrains.com/idea/), confirm every dialog, open the test class `NextHigherMountainTest`, change the path to your .gol file and click the green arrow.

## Libraries and data used
- Openstreetmap data under the [ODBL license](https://www.openstreetmap.org/copyright)
- The [GeoDesk library](https://docs.geodesk.com/tutorial)
- For other libraries see used the `pom.xml` file.

## Notes

- I made this mostly for my own trip planning, so it's in Czech and also other things are a bit Czechia-centric. The names of OSM objects in Czech are preferred. This is not a bug, but it's not difficult to change it in the source code. The code is in English including comments.
- I currently don't provide a runnable application, sorry.
- There is very little exception handling. It's a little app, if something is wrong, it just fails.