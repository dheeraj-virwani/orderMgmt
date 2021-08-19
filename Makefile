SBT := $(shell pwd)/extern/sbt-1.3.7/bin/sbt
NAME := order-mgmt

.PHONY: all clean compile run

all: clean compile run

clean:
	$(SBT) clean

compile:
	$(SBT) ";compile;package"

run:
	$(SBT) "runMain com.delivery.Main"

populate:
	$(SBT) "runMain com.delivery.DeliveryProducerApp"

test:
	$(SBT) test
