# WMOC class splitter

This project generates forest final start lists for WMOC according to the IOF rules. Potentially some other WMOC related special stuff added later.

Rules are here: https://orienteering.sport/orienteering/competition-rules/
This version tries to take into account all rules for version v1.21. Bug reports more than welcome :-)

To run the project, ensure you have Java 17 or newer & Maven installed and run `mvn jetty:run` and open [http://localhost:8080](http://localhost:8080) in browser.

Online version deployed temporarily to: https://wmoc-result-helper.dokku1.parttio.org


## TODO/things to figure out/not clear

### Normal final composition (SF/MF)

 * How to handle if situation if there are a lot of non-positioned qualifiers? E.g. M55 2022. In Spring qualification 28 mp/dsq/dns. Rules don't seem to be clear how to compose last final(s). In 2022, the B final is not filled with non-positioned and only contains 72 runners. C then contains all non-positioned runners. The weird thing is the there are 30 in C, not 28, didn't check who are the extras, if it was possible to sign up only for final 🤷‍. Interpreting the rules to me it would seem there shouldn't be C, but large B with all non-positioned runners. Should there be a rule/code for too large last final 🤔
 * Why 2022 Sprint M60B only has 79?
 * Is there some rule for assigning non-positioned runners to some specific order? 


