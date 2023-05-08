from gensim.models.callbacks import CallbackAny2Vec


class SaveCallback(CallbackAny2Vec):
    """A simple callback that saves the model after a given number of epochs."""

    def __init__(self, filename, epoch_count=1):
        self.filename = filename
        self.epoch = 0
        self.epoch_count = epoch_count

    def on_epoch_begin(self, model):
        self.epoch += 1

    def on_epoch_end(self, model):
        if self.epoch % self.epoch_count == 0:
            model.save(str(f"{self.filename}_{model.vector_size}_{self.epoch}_epochs"))
