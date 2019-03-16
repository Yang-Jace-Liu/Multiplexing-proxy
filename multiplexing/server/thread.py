import queue
import select
import socket
import time
from threading import Thread, Lock

from multiplexing.logger import getLogger
from multiplexing.server.data import Data


class LinkThread(Thread):

    def __init__(self, sock: socket.socket, addr, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.logger = getLogger(type(self).__name__)

        self.socket = sock
        self.remote = addr
        self.mutex = Lock()
        self.tasks = queue.Queue()

        # Set the socket to non-blocking
        self.socket.setblocking(False)

    def run(self):
        self.logger.debug("Connected from " + str(self.remote))

        while True:
            if self.socket is None and self.tasks.empty():
                time.sleep(0.01)
                continue
            readable, writeable, exceptional = select.select([self.socket], [self.socket], [])
            for sock in readable:
                data, addr = sock.recvfrom(4096)
                self.logger.debug("Received from " + str(sock.getpeername()) + ", Length: " + str(len(data)))
                Data.service_thread.add_task(data)
            if not self.tasks.empty():
                for sock in writeable:
                    with self.mutex:
                        task = self.tasks.get()
                    sock.sendall(task)

    def add_task(self, task):
        with self.mutex:
            self.tasks.put(task)


class ServiceThread(Thread):

    def __init__(self, host, port, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.logger = getLogger(type(self).__name__)

        self.host = host
        self.port = port
        self.socket = None  # type: socket.socket
        self.tasks = queue.Queue()

        self.mutex = Lock()

    def connect(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)  # type: socket.socket
        self.socket.connect((self.host, self.port))
        self.socket.setblocking(False)
        self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    def add_task(self, task):
        with self.mutex:
            self.tasks.put(task)

    def run(self):
        while True:
            if self.socket is None and self.tasks.empty():
                time.sleep(0.01)
                continue
            if self.socket is None:
                self.connect()
            readable, writeable, exceptional = select.select([self.socket], [self.socket], [])
            for sock in readable:
                data, addr = sock.recvfrom(4096)
                if (len(data) <= 0):
                    self.socket = None
                    continue
                self.logger.debug("Received from " + str(sock.getpeername()) + ", Length: " + str(len(data)))
                Data.send_to_link_thread(data)
            if not self.tasks.empty():
                for sock in writeable:
                    with self.mutex:
                        task = self.tasks.get()
                    sock.sendall(task)
