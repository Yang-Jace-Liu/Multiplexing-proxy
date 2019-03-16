import socket

from multiplexing.logger import getLogger
from multiplexing.client.config import Config
from multiplexing.client.data import Data
from multiplexing.client.thread import LinkThread, ClientThread


class Client:
    def __init__(self, config: Config):
        self.config = config
        self.logger = getLogger(type(self).__name__)

    def start(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind(("0.0.0.0", self.config.proxy_port))
        s.listen()

        try:
            for i in range(self.config.num_connections):
                thread = LinkThread(self.config.server_host, self.config.server_port)
                thread.start()
                Data.link_thread.append(thread)
            while True:
                conn, addr = s.accept()
                thread = ClientThread(conn, addr)
                thread.start()
                Data.client_thread = thread
                Data.client_thread.join()
        except KeyboardInterrupt:
            Data.client_thread.disconnect()
            for thread in Data.link_thread:
                thread.disconnect()
            Data.client_thread.join()
            for thread in Data.link_thread:
                thread.join()

            s.shutdown(socket.SHUT_RDWR)
            s.close()
            print("Bye")
