# Order Management

This is a basic Order Management Solution.
It consumes orders and couriers from underlying kafka topics `delivery.order` and `delivery.courier`
and akka-kafka stream in `DeliveryPipeline`

`DeliveryManager` Actor maintains these states and assigns courier to orders upon receiving any new Order

`DeliveryAPI` provides support for Rest APIs for Marking a courier as Available/Unavailable, 
fetch Orders for a courier and fetch courier for an Order

**_Steps to run_**

Please have zookeeper and kafka installed before this

1. Start zookeeper and start kafka on default ports
2. Start Main.scala or use below command from the project root directory
    `make all`
3. Use DeliveryProducerApp to produce some dummy kafka Orders and Couriers
    On another terminal -
    `make populate`
4. Verify the in-memory state using Rest APIs
    Here is a list of some useful APIs -
    
    To retrieve te entire list of couriers, orders and assignments -
    _http://localhost:8080/api/status_ 
    
    To mark a courier as avaiable/unavailable -
    _curl -XPOST -H "Content-Type: application/json" -d '{"<availability>"}' localhost:8080/api/courier/<courier_id>_
    
    To retieve the assigned courier for an order -
    _http://localhost:8080/api/courier?order=<order_id>_
    
    To retrieve the list of orders, a courier has to deliver -
    _http://localhost:8080/api/orders?courier=<courier_id>_

5. Testing
    `make test`
