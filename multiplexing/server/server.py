import socket

from multiplexing import logger
from multiplexing.server.Data import Data

from multiplexing.server.config import Config
from multiplexing.server.threads import LinkThread, WriterThread, ServiceThread


class Server:
    def __init__(self, config):
        self.logger = logger.getLogger("Main")
        self.config = config  # type: Config
        pass

    def start(self):
        serversocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        serversocket.bind(('0.0.0.0', self.config.proxy_port))
        serversocket.listen(100)

        Data().writer_thread = WriterThread()  # type: WriterThread
        Data().writer_thread.start()

        Data().service_thread = ServiceThread(self.config)
        Data().service_thread.start()

        while True:
            connection, address = serversocket.accept()
            self.logger.debug("Accepted connection from " + str(address))
            thread = LinkThread(connection, address)
            thread.start()
            Data().link_threads.append(thread)
