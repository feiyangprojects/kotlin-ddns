# Documentation

## Configuration

```json
{
  "mode": "oneshot",
  "interval": 0,
  "records": {
  }
}
```

> Properties

**mode**

Entries:

- `oneshot`: run tasks for once and stop
- `simple`: run tasks repeatedly

**interval**

Interval after a round of tasks is executed in `simple` mode, in millisecond

**records**

Type: Array\<[Record](#record)\>

Array of record tasks for updating

### Record

#### RecordBasic

```json
{
  "type": "basic",
  "name": "",
  "address": {
  },
  "username": "",
  "password": "",
  "endpoint": "",
  "name_parameter": "",
  "address_parameter": "",
  "other_parameters": {
  }
}
```

> Properties

**name**

Name of a record

**address**

Type: [RecordAddress](#recordaddress)

Address to monitor and update

**username**

Username for update authentication

**password**

Password for update authentication

**endpoint**

HTTP endpoint of DynDNS API

**name_parameter**

Examples: `domains`, `hostname`

Parameter to set record name

**address_parameter**

Examples: `ip`, `myip`, `myip6`

Parameter to set record address

**other_parameters**

Map of parameters to be included in the updating request

#### RecordAddress

##### RecordAddressBasic

```json
{
  "type": "basic",
  "endpoint": ""
}
```

> Properties

**endpoint**

Examples: `https://ipinfo.io/ip`, `https://v6.ipinfo.io/ip`

HTTP endpoint that returns an IP address

##### RecordAddressRegex

```json
{
  "type": "regex",
  "pattern": ""
}
```

> Properties

**pattern**

Examples: `^192\.0\.2\..+`, `^2001:db8:.+`

Regex pattern to filter IP address from network interface

## Compatibility

### Basic

#### DuckDNS

Set random `username` and `password` then include your token with `token` as parameter name in `other_parameters`

#### deSEC.io

Set the other stack (e.g. when updating IPv4 with `myip`, use `myip6`) to `preserve` in `other_parameters` to preserve records for both stacks
