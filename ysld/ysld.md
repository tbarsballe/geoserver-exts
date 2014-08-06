# Ysld

The following is an outline of the Ysld language:

    name: <text>
    title: <text>
    abstract: <text>
    feature-styles:
    - name: <text>
      title: <text>
      abstract: <text>
      transform:
        name: <text>
        params: <options>
      rules:
      - name: <text>
        title: <text>
        abstract: <text>
        scale: <tuple>
        filter: <filter>
        else: <bool>
        symbolizers:
        - point: &graphic
            symbols:
            - mark:
                shape: <shape>
                fill: &fill
                  color: <color>
                  opacity: <expression>
                  graphic: *graphic
                stroke: &stroke
                  color: <color>
                  width: <expression>
                  opacity: <expression>
                  linejoin: <expression>
                  linecap: <expression>
                  dasharray: <float[]>
                  dashoffset: <expression>
            - external:
                url: <text>
                format: <text>
            anchor: <tuple>
            displacement: <tuple>
            opacity: <expression>
            rotation: <expression>
            size: <expression>
            options: <options>
            gap: <expression>
            initial-gap: <expression>
        - line: 
            stroke: *stroke
            offset: <expression>
            options: <options>
        - polygon:
            fill: *fill
            stroke: *stroke
            offset: <expression>
            displacement: <tuple>
        - raster: 
            color-map: 
              type: ramp|intervals|values
              entries:
              - <quad> # color, opacity, value, label
            opacity: <expression>
            contrast-enhancement: 
              mode: normalize|histogram|none
              gamma: <expression>
        - text:
            label: <expression>
            placement:
              type: point|line
              offset: <expression>
              anchor: <tuple>
              displacement: <tuple>
              rotation: <expression>
            font:
              family: <expression>
              size: <expression>
              style: <expression>
              weight: <expression>
            fill: *fill;
            options: <options>

<a name="expression"></a>

## Expressions

Expressions are specified as CQL/ECQL parse-able expression strings. See the 
[cql_docs] and this [cql_tutorial] for more information about the CQL syntax. 

[cql_docs]: http://docs.geotools.org/stable/userguide/library/cql/ecql.html "CQL documentation"
[cql_tutorial]: http://docs.geoserver.org/latest/en/user/tutorials/cql/cql_tutorial.html "CQL tutorial"

The following are some simple examples:

### Literals

    stroke:
      width: 10
      linecap: 'butt'

Note: Single quotes are needed for string literals to differentiate them from
attribute references. 

### Attributes

    text:
      label: [STATE_NAME]

### Functions

    point:
      rotation: sqrt([STATE_POP])

## Filters

Rule filters are specified as CQL/ECQL parse-able filters. A simple example:

    rules:
    - filter: [type] = 'highway'
      symbolizers:
      - line:
          stroke:
            width: 5

See the [cql_docs] and this [cql_tutorial] for more information about the CQL 
syntax. 

## Tuples

Some attributes are specified as pairs. For example:

    rules:
    - scale: (10000,20000)

    point:
      anchor: (0.5,0.5)

One of the values in the tuple may be omitted as in:

    rules:
    - scale: (,10000)
    - scale: (10000,)

## Options

Symbolizer options are specified as normal mappings on a symbolizer object. 
For example:

    text:
      options:
        followLine: true
        maxAngleDelta: 90
        maxDisplacement: 400
        repeat: 150

## Arrays

Lists and arrays are specified as space delimited. For example:

    stroke:
      dasharray: 5 2 1 2

## Anchors & References

With Yaml it is possible to reference other parts of a document. With this 
it is possible to support variables and mix ins. An example of a color variable:

    redish: &redish #DD0000
    point:
      fill:
        color: *redish

An named "anchor" is declared with the `&` character and then referenced with 
the `*` character. This same feature can be used to do "mix-ins" as well:

    define: &highway_zoom10
      scale: (10000,20000)
      filter: type = 'highway'

    rules:
    - >>: *highway_zoom10
      symbolizers:
      - point

The syntax in this case is slightly different and is used when referencing an 
entire mapping object rather than just a simple scalar value. 
