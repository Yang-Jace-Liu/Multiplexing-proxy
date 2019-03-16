import queue
import select
import socket
import time
from threading import Thread, Lock

from multiplexing.client.data import Data
from multiplexing.logger import getLogger


class LinkThread(Thread):

    def __init__(self, host, port, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.logger = getLogger(self.name)

        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.host = host
        self.port = port
        self.mutex = Lock()
        self.tasks = queue.Queue()

        self.end = False

        # Set the socket to non-blocking
        self.socket.setblocking(False)

    def disconnect(self):
        self.socket.shutdown(socket.SHUT_RDWR)
        self.socket.close()

    def connect(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)  # type: socket.socket
        self.socket.connect((self.host, self.port))
        self.socket.setblocking(False)
        self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    def run(self):
        self.connect()
        self.logger.debug("Connect to " + str(self.host) + ":" + str(self.port))

        while True:

            if self.end:
                self.socket.shutdown(socket.SHUT_RDWR)
                self.socket.close()
                return
            readable, writeable, exceptional = select.select([self.socket], [self.socket], [])
            for sock in readable:
                data, addr = sock.recvfrom(4096)
                self.logger.debug("Received from " + str(sock.getpeername()) + ", Length: " + str(len(data)))
                Data.client_thread.add_task(data)
            if not self.tasks.empty():
                for sock in writeable:
                    with self.mutex:
                        task = self.tasks.get()
                    sock.sendall(task)

    def add_task(self, task):
        with self.mutex:
            self.tasks.put(task)


class ClientThread(Thread):

    def __init__(self, sock, addr, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.logger = getLogger(self.name)

        self.socket = sock
        self.addr = addr
        self.tasks = queue.Queue()

        self.end = False

        self.mutex = Lock()

    def add_task(self, task):
        with self.mutex:
            self.tasks.put(task)

    def disconnect(self):
        self.end = True

    def run(self):
        while True:
            if self.end:
                self.socket.shutdown(socket.SHUT_RDWR)
                self.socket.close()
                return
            readable, writeable, exceptional = select.select([self.socket], [self.socket], [])
            for sock in readable:
                data, addr = sock.recvfrom(4096)
                if len(data) <= 0:
                    self.logger.debug("Disconnected from " + str(sock.getpeername()))
                    return
                self.logger.debug("Received from " + str(sock.getpeername()) + ", Length: " + str(len(data)))
                Data.send_to_link_thread(data)
            if not self.tasks.empty():
                for sock in writeable:
                    with self.mutex:
                        task = self.tasks.get()
                    sock.sendall(task)
