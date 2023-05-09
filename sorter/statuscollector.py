import logging
from enum import Enum


class Status(Enum):
    Started = 10
    JSONParsed = 20
    OriginalExtracted = 30
    FilesPatched = 40
    Vectorized = 50
    Compared = 60
    JSONUpdated = 70
    Done = 80


class StatusCollector:
    def initLogger(self, fileLogLevel, streamLogLevel):
        """Initializes a logger object to write to the console an into a logfile.

        Parameters
        ----------
        fileLogLevel : int (Logger level)
            The logging level for the filehandler.
        streamLogLevel : int (Logger level)
            The logging level for the streamhandler.
        """
        self.logger = logging.getLogger(__name__)
        self.logger.setLevel(logging.DEBUG)
        fileHandler = logging.FileHandler("sorter.log")
        formatter = logging.Formatter(
            "%(asctime)s:%(name)s:%(levelname)s:%(message)s",
            datefmt="%Y-%m-%d %H:%M:%S",
        )
        fileHandler.setFormatter(formatter)
        fileHandler.setLevel(fileLogLevel)
        streamHandler = logging.StreamHandler()
        streamHandler.setFormatter(formatter)
        streamHandler.setLevel(streamLogLevel)
        if len(self.logger.handlers) == 0:
            self.logger.addHandler(fileHandler)
            self.logger.addHandler(streamHandler)

    def __init__(self, fileLogLevel=logging.DEBUG, streamLogLevel=logging.ERROR):
        """Initializes the logger and logs the start of the program.

        Parameters
        ----------
        fileLogLevel : int, optional
            The logging level for the filehandler, by default logging.DEBUG
        streamLogLevel : int, optional
            The logging level for the streamhandler, by default logging.ERROR
        """
        self.initLogger(fileLogLevel, streamLogLevel)
        self.status = Status.Started
        self.logger.info("Status: %s.", self.status.name)

    def setStatus(self, newStatus):
        """Sets the status and logs the status change.

        Parameters
        ----------
        newStatus : Status
            The new status
        """
        self.logger.info(
            "Status changed from %s to %s.", self.status.name, newStatus.name
        )
        self.status = newStatus

    def info(self, message):
        """Logs an info log entry

        Parameters
        ----------
        message : str
            The message to be logged
        """
        self.logger.info(message)

    def error(self, message):
        """Logs an error log entry

        Parameters
        ----------
        message : str
            The message to be logged
        """
        self.logger.error(message)

    def exception(self, message):
        """Logs an error log entry with the message and a debug log entry with the message and the exception info

        Parameters
        ----------
        message : str
            The message to be logged
        """
        self.logger.error(message)
        self.logger.debug(message, exc_info=True)

    def debug(self, message):
        """Logs a debug log entry

        Parameters
        ----------
        message : str
            The message to be logged
        """
        self.logger.debug(message)
