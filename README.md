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
2. See the test class about the parameters needed.
 
But since you asked, the order of parameters is `<path> <query>`. E.g.:

`"/home/mememe/Czechia.gol" "n[place=city,town,village,hamlet][name][population>=100000]`

will show charts of next higher mountains for all cities in Czechia that have over 100 000 citizens and have a name. Which is not many.

How to do queries is described in the [GeoDesk pages](https://docs.geodesk.com/tutorial).

If you don't know Java, I would recommend to open this project in a smart Java editor, e.g. [IntelliJ Idea](https://www.jetbrains.com/idea/), confirm every dialog, open the test class `NextHigherMountainTest`, change the path to your .gol file and click the green arrow.

## Libraries and data used
- Openstreetmap data under the [ODBL license](https://www.openstreetmap.org/copyright)
- The [GeoDesk library](https://docs.geodesk.com/tutorial)
- For other libraries see used the `pom.xml` file.

## Notes

- I made this mostly for my own trip planning, so it's a bit Czechia-centric. Mainly the names of OSM objects in Czech are preferred. This is not a bug, but it's not difficult to change it in the source code.
- I don't provide a runnable application, sorry.
- There is very little to none exception handling. It's a small app, if something is wrong, it just fails.