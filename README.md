# ledger-fetch

A Clojure interface to automate the collection of CSV statements from several banks with the purpose of importing them into [`ledger-cli`](http://ledger-cli.org).

## Usage

`config.edn` will need to be populated for all banks that will be interfaced with.

So far, I've settled on `:username`, `:password`, and `:challenge` as attributes that I hope will meet the criteria for most bank login systems. `:challenge` is a vector of maps that contain both the security question and matching answer.

I hope that in the future I'll have a better answer to the credentials problem that doesn't involve having a non-encrypted, publicly readable file.

## License

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
