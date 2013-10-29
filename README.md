# butler

A small ring like node.js application written with core.async. It is supposed to manage repository logic around 'geschichte' especially general read access but restricted write access to write-once new hash keys.

## Usage

Build clojurescript and run nodejs on compiled js file:

```
lein cljsbuild once
nodejs resources/public/js/main.js COUCHDB-NAME
```

## License

Copyright © 2013 Konrad Kühne und Christian Weilbach

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
