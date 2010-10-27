PlatformKit Servlet
===================

Web interface sub-project
-------------------------

Maintainer: Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
License: EPL (see http://www.eclipse.org/legal/epl-v10.html)

Includes a servlet interface.

Requirements
------------

- Servlet requires a Jetty application server for deployment.
  Dynamic deployment has been tested with Jetty-6.1.4.
- Fact++ includes native libraries. Jetty must be started using
  the "-Djava.library.path=..." directive, pointing at the
  location of the native Fact++ libraries.