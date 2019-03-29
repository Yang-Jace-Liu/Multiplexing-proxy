import socket

from multiplexing import logger

from multiplexing.client.config import Config
from multiplexing.client.threads import SessionThread


class Client:
    def __init__(self, config: Config):
        self.config = config
        self.serversocket = None
        self.logger = logger.getLogger(Client.__name__)

    def start(self) -> None:
        self.serversocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.serversocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.serversocket.bind(('0.0.0.0', self.config.listening_port))
        self.serversocket.listen(100)

        while True:
            sock, remote = self.serversocket.accept()
            self.logger.debug("Connected from " + str(remote))
            session_thread = SessionThread(sock, remote, self.config)
            session_thread.start()
