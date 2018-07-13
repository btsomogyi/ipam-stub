##IPAM Exercise

Based on specification provided [here](https://gist.github.com/mdlayher/a799a98899855399ed999431a66d1b12)

### Basic Features
- Uses Java HTTP Server for basic front-end
- Abstracted persistence layer to facilitate alternative storage implementations
- **IpamSubnet** class implements an IPV4 and IPV6 compatible wrapper class around Java InetAddress (Apache SubnetUtils not yet supporting IPV6)
- Use of Guava modules:
-- InetAddresses
-- Preconditions

### Testing Approach
- Uses JUnit and Mockito to functional test classes