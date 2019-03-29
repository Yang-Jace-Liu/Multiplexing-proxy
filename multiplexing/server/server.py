import socket
import re

from multiplexing import logger
from multiplexing.server.config import Config
from multiplexing.server.threads import LinkThread, SessionThread


class Server:
    def __init__(self, config):
        self.logger = logger.getLogger("Main")
        self.config = config  # type: Config

        self.session_threads = {}

    def start(self):
        serversocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        serversocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        serversocket.bind(('0.0.0.0', self.config.proxy_port))
        serversocket.listen(100)

        pattern = re.compile("^Session-(\\d+):Link-(\\d+)$")

        while True:
            connection, address = serversocket.accept()
            self.logger.debug("Accepted connection from " + str(address))
            buf_str = connection.recv(1024).decode("utf-8")
            result = pattern.match(buf_str)
            session, link = (int(result.group(1)), int(result.group(2)))
            if session not in self.session_threads.keys():
                self.session_threads[session] = SessionThread(self.config)
                self.logger.debug("Create a new session: " + str(session))
            self.session_threads[session].add_link(connection, address)
