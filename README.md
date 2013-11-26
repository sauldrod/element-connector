element-connector
=================

[![Build Status](https://api.travis-ci.org/mkovatsc/element-connector.png?branch=master)](https://travis-ci.org/mkovatsc/element-connector)

The element-connector is a Java socket abstraction for UDP, DTLS, TCP, etc.
It is used to modularize Californium (Cf) and add DTLS support through the
standalone Scandium (Sc) project. Further projects can add so add different
transports independently (e.g., TCP, SMS, or special sockets when running in
an optimized VM such as Virtenio's PreonVM).

Maven
-----

Use `mvn clean install` in the Cf root directory to build everything.
Standalone JARs of the examples will be copied to ./run/.
(For convenience they are directly included in the Git repository.)

The Maven repositories are:

* [https://github.com/mkovatsc/maven/raw/master/releases/](https://github.com/mkovatsc/maven/raw/master/releases/)
* [https://github.com/mkovatsc/maven/raw/master/snapshots/](https://github.com/mkovatsc/maven/raw/master/snapshots/)

Eclipse
-------

The project also includes the project files for Eclipse. Make sure to have the
following before importing the Californium (Cf) projects:

* [Eclipse EGit](http://www.eclipse.org/egit/)
* [m2e - Maven Integration for Eclipse](http://www.eclipse.org/m2e/)
* UTF-8 workspace text file encoding (Preferences &raquo; General &raquo; Workspace)

Then choose *[Import... &raquo; Git &raquo; Projects from Git &raquo; Local]*
to import Californium into Eclipse.
