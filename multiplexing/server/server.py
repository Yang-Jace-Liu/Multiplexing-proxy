import re
import socket
from threading import Thread, Lock

from multiplexing import logger
from multiplexing.server.config import Config
from multiplexing.server.threads import SessionThread


class Server:
    ID_PATTERN = re.compile("^Session-(\\d+):Link-(\\d+)$")

    def __init__(self, config):
        self.logger = logger.getLogger("Main")
        self.config = config  # type: Config

        self.session_threads = {}

        self.lock = Lock()

    def start(self):
        serversocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        serversocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        serversocket.bind(('0.0.0.0', self.config.proxy_port))
        serversocket.listen(100)

        Thread(target=self.second_port).start()

        while True:
            connection, address = serversocket.accept()
            self.accept(connection, address, self.config.proxy_port)

    def accept(self, connection, address, port):
        self.logger.debug("[" + str(port) + "] Accepted connection from " + str(address))
        buf_str = connection.recv(1024).decode("utf-8")
        self.logger.debug(buf_str)
        result = Server.ID_PATTERN.match(buf_str)
        session, link = (int(result.group(1)), int(result.group(2)))
        with self.lock:
            if session not in self.session_threads.keys():
                self.session_threads[session] = SessionThread(self.config)
                self.logger.debug("[" + str(port) + "] Create a new session: " + str(session))
            self.session_threads[session].add_link(connection, address)

    def second_port(self):
        serversocket2 = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        serversocket2.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        serversocket2.bind(('0.0.0.0', self.config.proxy_port + 1))
        serversocket2.listen(100)

        while True:
            connection, address = serversocket2.accept()
            self.accept(connection, address, self.config.proxy_port + 1)
