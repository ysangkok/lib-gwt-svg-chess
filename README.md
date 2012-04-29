Start WebSocket server that the XBoard client can connect to
------------------------------------------------------------

Write ``runserver.sh`` in the ``websockify`` directory:

    #!/bin/sh
    nc -q -1 -k -l 2023 | tee /dev/null

Remember ``chmod +x runserver.sh``.

Start ``xboard`` from ``websockify`` as working directory like this:

    ./websockify 2023 -- xboard -fcp "./runserver.sh"

XBoard launches. Do not do anything in XBoard before you have connected ``lib-gwt-svg-chess``! After connecting (i.e. clicking "OK" after specifying server IP/port in the JavaScript prompt that appears when you change the game mode to XBoard), make the first move in XBoard as white. The move should appear in ``lib-gwt-svg-chess`` and you can now play as black from there.

If there are problems, try connecting (i.e. prompt "OK" click) within two seconds of starting XBoard (with the wrapper above). The reason is that XBoard is trying to do some initialization that it expects the engine to reply to; but on my machine it seems to work even if you connect the WebSocket client (i.e. XBoard engine) only after the XBoard initialization.