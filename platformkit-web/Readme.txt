PlatformKit Servlet
===================

Web interface sub-project
-------------------------

Maintainer: Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
License: EPL (see http://www.eclipse.org/legal/epl-v10.html)

Includes a servlet interface.

Requirements
------------

- Servlet requires the included version of xercesImpl.jar to be
  loaded, since Jena depends on it. If the application server
  already loads xercesImpl.jar by default, replace it by the
  included version.