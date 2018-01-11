# Lab 10: Comments

1. No conversion for getBusService/getBusStops/routesToStopsNearby
Many students couldn't cover all of them. Maybe they didn't notice they all contain web queries.

2. No handling for findDirectBusServicesBetween
Many students lost this 1 mark because they didn't take care of the returned CF from findDirectBusServicesBetween in findBusServicesWithTransferBetween.

3. Confusion between parallelism and asynchronization
Some students use one CF to wrap the whole function. Inside the function, they use stream.parallel as replacement of for loop.

4. Join all CFs together
I think a number of student call join right after a CF within the loop, which is no difference than a synchronous call. I didn't give penalty for this deficiency but have pointed out in the pdf.

Thanks!

Best regards,
Mingyang

`findDirectBusServicesBetween`:

- -0.5: No conversion for `getBusServices`
- -0.5: Missing join for `hasStopAt`

`findBusServicesToNearby`:

- -0.5: No conversion for `getBusServices`
- -0.5: No conversion for `routesToStopsNearby`
- -0.5: Missing join for `routesToStopsNearby`

`findBusServicesWithTransferBetween`:

- -0.5: No conversion for `getBusServices`
- -0.5: No conversion for `getBusStops`
- -1: No handling for `findDirectBusServicesBetween`
