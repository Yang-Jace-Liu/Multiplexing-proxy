import socket

from multiplexing.logger import getLogger
from multiplexing.server.config import Config
from multiplexing.server.data import Data
from multiplexing.server.thread import LinkThread, ServiceThread


class Server:
    def __init__(self, config: Config):
        self.config = config
        self.logger = getLogger(type(self).__name__)

    def start(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind(("0.0.0.0", self.config.proxy_port))
        s.listen()

        Data.service_thread = ServiceThread(self.config.service_host, self.config.service_port, daemon=True)
        Data.service_thread.start()

        try:
            while True:
                conn, addr = s.accept()
                thread = LinkThread(conn, addr, daemon=True)
                thread.start()
                Data.link_thread.append(thread)
        except KeyboardInterrupt:
            s.shutdown(socket.SHUT_RDWR)
            s.close()
            print("Bye")
