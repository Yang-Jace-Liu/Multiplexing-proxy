from queue import Queue
from threading import Thread, Event, Lock
import socket

from multiplexing import logger
from multiplexing.server.Data import Data


class LinkThread(Thread):
    def __init__(self, connection, address):
        super().__init__()
        self.logger = logger.getLogger(self.name)

        self.socket = connection  # type: socket.socket
        self.remote = address
        self.writer_thread = Data().writer_thread  # type:WriterThread

    def run(self):
        while True:
            buf = self.socket.recv(8192)
            if len(buf) <= 0:
                self.logger.debug("Disconnected from " + str(self.remote))
                return
            self.logger.debug("Received from " + str(self.remote))
            self.writer_thread.add_task(WriterThread.TARGET_SERVICE, buf)


class WriterThread(Thread):
    TARGET_SERVICE = 1
    TARGET_CLIENT = 2

    def __init__(self):
        super().__init__()
        self.event = Event()
        self.queue = Queue()
        self.lock = Lock()

    def run(self):
        while True:
            self.event.wait()

            with self.lock:
                task = self.queue.get()  # type: tuple[int, bytes]

            if task[0] == WriterThread.TARGET_CLIENT:
                thread = self.choose_link()
                thread.socket.send(task[1])
            else:
                thread = Data().service_thread  # type: ServiceThread
                thread.send(task[1])

            if self.queue.empty():
                self.event.clear()

    def add_task(self, target, load):
        with self.lock:
            self.queue.put((target, load))
        self.event.set()

    def choose_link(self) -> LinkThread:
        return Data().link_threads[0]


class ServiceThread(Thread):
    def __init__(self, config):
        super().__init__()
        self.config = config
        self.socket = None  # type: socket.socket
        self.writer_thread = Data().writer_thread  # type: WriterThread
        self.event = Event()

    def is_connected(self):
        return self.socket is not None

    def connect(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.connect((self.config.service_hostname, self.config.service_port))
        self.event.set()

    def run(self):
        while True:
            self.event.wait()

            buf = self.socket.recv(8192)
            if len(buf) <= 0:
                self.event.clear()
                self.socket = None
                continue
            self.writer_thread.add_task(WriterThread.TARGET_CLIENT, buf)

    def send(self, buf):
        if self.socket is None:
            self.connect()
        self.socket.send(buf)
