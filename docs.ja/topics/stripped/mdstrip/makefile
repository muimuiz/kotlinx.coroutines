COROUTINEJAR = ./kotlinx-coroutines-core-1.4.2.jar
MDSTRIP      = java -cp ./mdstrip.jar:$(COROUTINEJAR) MdstripKt

all: mdstrip.jar \
	../coroutines-guide.md \
	../basics.md \
	../cancellation-and-timeouts.md \
	../composing-suspending-functions.md \
	../coroutine-context-and-dispatchers.md \
	../flow.md \
	../channels.md \
	../exception-handling.md \
	../shared-mutable-state-and-concurrency.md

mdstrip.jar: mdstrip.kt $(COROUTINEJAR)
	kotlinc -cp $(COROUTINEJAR) mdstrip.kt -include-runtime -d mdstrip.jar

../coroutines-guide.md: ../../coroutines-guide.md mdstrip.jar
	$(MDSTRIP) $< > $@
../basics.md: ../../basics.md mdstrip.jar
	$(MDSTRIP) $< > $@
../cancellation-and-timeouts.md: ../../cancellation-and-timeouts.md mdstrip.jar
	$(MDSTRIP) $< > $@
../composing-suspending-functions.md: ../../composing-suspending-functions.md mdstrip.jar
	$(MDSTRIP) $< > $@
../coroutine-context-and-dispatchers.md: ../../coroutine-context-and-dispatchers.md mdstrip.jar
	$(MDSTRIP) $< > $@
../flow.md: ../../flow.md mdstrip.jar
	$(MDSTRIP) $< > $@
../channels.md: ../../channels.md mdstrip.jar
	$(MDSTRIP) $< > $@
../exception-handling.md: ../../exception-handling.md mdstrip.jar
	$(MDSTRIP) $< > $@
../shared-mutable-state-and-concurrency.md: ../../shared-mutable-state-and-concurrency.md mdstrip.jar
	$(MDSTRIP) $< > $@

