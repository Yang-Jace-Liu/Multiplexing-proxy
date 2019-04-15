import random
import socket
from queue import Queue
from threading import Thread, Event, Lock

from multiplexing import logger


class SessionThread(Thread):
    def __init__(self, config, name=None):
        super().__init__()
        if name is not None:
            self.name = name
        self.logger = logger.getLogger(self.name)
        self.config = config
        self.link_threads = []
        self.stop = Event()

        self.service_thread = ServiceThread(self, None, self.config)
        self.writer_thread = WriterThread(self, self.service_thread, self.link_threads)
        self.service_thread.writer_thread = self.writer_thread

        self.service_thread.start()
        self.writer_thread.start()

        self.seq = -1

    def add_link(self, socket, remote, name=None):
        link_thread = LinkThread(self, self.writer_thread, socket, remote, name)
        self.link_threads.append(link_thread)
        link_thread.start()

    def end(self):
        self.writer_thread.stop.set()
        self.service_thread.stop.set()
        for link_thread in self.link_threads:
            link_thread.stop.set()
        self.writer_thread.join()
        self.service_thread.join()
        for link_thread in self.link_threads:
            link_thread.join()
        self.logger.debug("Stop session")

    def run(self):
        self.stop.wait()
        self.end()

    def get_seq(self):
        self.seq += 1
        return self.seq


class LinkThread(Thread):
    def __init__(self, session_thread, writer_thread, connection, address, name=None):
        super().__init__()
        if name is not None:
            self.name = name
        self.logger = logger.getLogger(self.name)

        self.session_thread = session_thread  # type: SessionThread
        self.writer_thread = writer_thread  # type: WriterThread
        self.socket = connection  # type: socket.socket
        self.remote = address  # type: tuple[str, int]

        self.stop = Event()

    def end(self):
        self.socket.close()
        self.session_thread.stop.set()

    def run(self):
        self.socket.settimeout(60)
        while True:
            try:
                buf = self.socket.recv(8192)
                if len(buf) <= 0:
                    self.logger.debug("Disconnected from " + str(self.remote))
                    self.end()
                    return
                self.logger.debug("Received from " + str(self.remote))
                self.writer_thread.add_task(WriterThread.TARGET_SERVICE, buf)
            except socket.timeout as e:
                if self.stop.is_set():
                    self.end()
                    return

    def send(self, buf):
        self.socket.send(buf)


class WriterThread(Thread):
    TARGET_SERVICE = 1
    TARGET_CLIENT = 2

    def __init__(self, session_thread, service_thread, link_threads):
        super().__init__()
        self.event = Event()
        self.stop = Event()
        self.queue = Queue()
        self.lock = Lock()

        self.session_thread = session_thread
        self.service_thread = service_thread
        self.link_threads = link_threads

    def run(self):
        while True:
            self.event.wait(60)

            if self.stop.is_set():
                self.session_thread.stop.set()
                return

            if not self.event.is_set():
                continue

            with self.lock:
                task = self.queue.get()
                task[0].send(task[1])

            if self.queue.empty():
                self.event.clear()

    def add_task(self, target, load):
        if type(target) == int:
            if target == WriterThread.TARGET_CLIENT:
                target = self.choose_link()
            else:
                target = self.service_thread
            with self.lock:
                self.queue.put((target, load))
        self.event.set()

    def choose_link(self) -> LinkThread:
        l = len(self.link_threads)
        return self.link_threads[random.randint(0, l - 1)]
        # return self.link_threads[random.randint(0, 0)]


class ServiceThread(Thread):
    def __init__(self, session_thread, writer_thread, config):
        super().__init__()
        self.session_thread = session_thread
        self.config = config
        self.socket = None  # type: socket.socket
        self.writer_thread = writer_thread  # type: WriterThread
        self.event = Event()
        self.stop = Event()

    def connect(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.connect((self.config.service_hostname, self.config.service_port))
        self.event.set()

    def run(self):
        while True:
            self.event.wait(60)

            if self.stop.is_set():
                self.end()
                return

            if not self.event.is_set():
                continue

            buf = self.socket.recv(8192)
            if len(buf) <= 0:
                self.event.clear()
                self.socket = None
                continue
            self.writer_thread.add_task(WriterThread.TARGET_CLIENT, self.wrap(buf))

    def wrap(self, buf):
        l = len(buf)
        seq = self.session_thread.get_seq()
        print(seq)
        return seq.to_bytes(4, 'big') + l.to_bytes(4, 'big') + buf

    def send(self, buf):
        if self.socket is None:
            self.connect()
        self.socket.send(buf)

    def end(self):
        if self.socket is not None:
            self.socket.close()
        self.session_thread.stop.set()
