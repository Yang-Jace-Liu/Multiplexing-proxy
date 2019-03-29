import socket
from queue import Queue
from threading import Thread, Event

from multiplexing import logger
from multiplexing.client.config import Config


class SessionThread(Thread):
    id = 0

    @staticmethod
    def get_id():
        SessionThread.id += 1
        return SessionThread.id

    def __init__(self, socket: socket.socket, remote, config: Config) -> None:
        super().__init__()
        self.socket = socket
        self.remote = remote
        self.config = config

        self.link_threads = []  # type: list[LinkThread]
        self.write_thread = None  # type: WriterThread

        self.id = SessionThread.get_id()
        self.name = "Session-" + str(self.id)
        self.logger = logger.getLogger(self.name)

        self.stop = Event()

    def end(self):
        self.socket.close()
        if self.write_thread is not None:
            self.write_thread.stop.set()
        for link_thread in self.link_threads:
            link_thread.stop.set()
        if self.write_thread is not None:
            self.write_thread.join()
        for link_thread in self.link_threads:
            link_thread.join()

    def run(self):
        self.socket.settimeout(60)

        for i in range(self.config.link_num):
            self.link_threads.append(LinkThread(self, None, self.config))
        self.write_thread = WriterThread(self, self.link_threads)
        for link_thread in self.link_threads:
            link_thread.write_thread = self.write_thread
            link_thread.start()
        self.write_thread.start()

        while True:
            try:
                buf = self.socket.recv(8192)
                if len(buf) <= 0:
                    self.logger.debug("Disconnected")
                    self.end()
                    return
                self.write_thread.add_task(WriterThread.TARGET_REMOTE, buf)
            except TimeoutError as e:
                if self.stop.is_set():
                    self.end()
                    return


class LinkThread(Thread):
    id = 0

    @staticmethod
    def get_id():
        LinkThread.id += 1
        return LinkThread.id

    def __init__(self, session_thread: SessionThread, write_thread, config: Config):
        super().__init__()
        self.session_thread = session_thread
        self.write_thread = write_thread  # type: WriterThread
        self.config = config

        self.name = "Session-" + str(self.session_thread.id) + ":Link-" + str(self.id)
        self.logger = logger.getLogger(self.name)
        self.socket = None  # type: socket.socket

        self.id = LinkThread.get_id()
        self.stop = Event()

    def connect(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.settimeout(60)
        self.socket.connect((self.config.proxy_server_hostname, self.config.proxy_server_port))

    def run(self):
        self.connect()
        while True:
            try:
                buf = self.socket.recv(8192)
                if len(buf) <= 0:
                    self.logger.debug("Disconnected")
                    self.socket.close()
                    return
                self.write_thread.add_task(self.session_thread, buf)
            except TimeoutError as e:
                if self.stop.is_set():
                    self.socket.close()
                    return


class WriterThread(Thread):
    TARGET_SESSION = 1
    TARGET_REMOTE = 2

    def __init__(self, session_thread: SessionThread, link_threads):
        super().__init__()

        self.stop = Event()
        self.event = Event()

        self.tasks = Queue()

        self.session_thread = session_thread
        self.link_threads = link_threads

        for link_thread in link_threads:
            self.add_task(link_thread, link_thread.name)
        self.event.set()

    def choose_thread(self):
        return self.link_threads[0]

    def add_task(self, target, buf):
        if type(target) == int:
            if target == WriterThread.TARGET_SESSION:
                self.tasks.put((self.session_thread, buf))
            else:
                self.tasks.put((self.choose_thread(), buf))
        else:
            self.tasks.put((target, buf))

    def run(self):
        while True:
            self.event.wait(60)
            if self.stop.is_set():
                return
            if not self.event.is_set():
                continue

            thread, buf = self.tasks.get()
            thread.socket.send(buf)

            if self.tasks.empty():
                self.event.clear()
