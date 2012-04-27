Start WebSocket server that the XBoard client can connect to
------------------------------------------------------------

Write ``runserver.sh`` in the ``websockify`` directory:

    #!/bin/sh
    nc -q -1 -k -l 2023 | tee /dev/null

Remember ``chmod +x runserver.sh``.

Start ``xboard`` from ``websockify`` as working directory like this:

    ./websockify 2023 -- xboard -fcp "./runserver.sh"
