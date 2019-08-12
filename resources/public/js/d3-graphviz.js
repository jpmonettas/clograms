(function (global, factory) {
	typeof exports === 'object' && typeof module !== 'undefined' ? factory(exports, require('d3-selection'), require('d3-dispatch'), require('d3-transition'), require('d3-timer'), require('d3-interpolate'), require('d3-zoom'), require('viz.js/viz'), require('d3-format'), require('d3-path')) :
	typeof define === 'function' && define.amd ? define(['exports', 'd3-selection', 'd3-dispatch', 'd3-transition', 'd3-timer', 'd3-interpolate', 'd3-zoom', 'viz.js/viz', 'd3-format', 'd3-path'], factory) :
	(factory((global['d3-graphviz'] = {}),global.d3,global.d3,global.d3,global.d3,global.d3,global.d3,global.Viz,global.d3,global.d3));
}(this, (function (exports,d3,d3Dispatch,d3Transition,d3Timer,d3Interpolate,d3Zoom,Viz,d3Format,d3Path) { 'use strict';

Viz = Viz && Viz.hasOwnProperty('default') ? Viz['default'] : Viz;

function extractElementData(element) {

    var datum = {};
    var tag = element.node().nodeName;
    datum.tag = tag;
    if (tag == '#text') {
        datum.text = element.text();
    } else if (tag == '#comment') {
        datum.comment = element.text();
    }
    datum.attributes = {};
    var attributes = element.node().attributes;
    if (attributes) {
        for (var i = 0; i < attributes.length; i++) {
            var attribute = attributes[i];
            var name = attribute.name;
            var value = attribute.value;
            datum.attributes[name] = value;
        }
    }
    var transform = element.node().transform;
    if (transform && transform.baseVal.numberOfItems != 0) {
        var matrix = transform.baseVal.consolidate().matrix;
        datum.translation = { x: matrix.e, y: matrix.f };
        datum.scale = matrix.a;
    }
    if (tag == 'ellipse') {
        datum.center = {
            x: datum.attributes.cx,
            y: datum.attributes.cy
        };
    }
    if (tag == 'polygon') {
        var points = element.attr('points').split(' ');
        var x = points.map(function (p) {
            return p.split(',')[0];
        });
        var y = points.map(function (p) {
            return p.split(',')[1];
        });
        var xmin = Math.min.apply(null, x);
        var xmax = Math.max.apply(null, x);
        var ymin = Math.min.apply(null, y);
        var ymax = Math.max.apply(null, y);
        var bbox = {
            x: xmin,
            y: ymin,
            width: xmax - xmin,
            height: ymax - ymin
        };
        datum.bbox = bbox;
        datum.center = {
            x: (xmin + xmax) / 2,
            y: (ymin + ymax) / 2
        };
    }
    if (tag == 'path') {
        var d = element.attr('d');
        var points = d.split(/[A-Z ]/);
        points.shift();
        var x = points.map(function (p) {
            return +p.split(',')[0];
        });
        var y = points.map(function (p) {
            return +p.split(',')[1];
        });
        var xmin = Math.min.apply(null, x);
        var xmax = Math.max.apply(null, x);
        var ymin = Math.min.apply(null, y);
        var ymax = Math.max.apply(null, y);
        var bbox = {
            x: xmin,
            y: ymin,
            width: xmax - xmin,
            height: ymax - ymin
        };
        datum.bbox = bbox;
        datum.center = {
            x: (xmin + xmax) / 2,
            y: (ymin + ymax) / 2
        };
        datum.totalLength = element.node().getTotalLength();
    }
    if (tag == '#text') {
        datum.text = element.text();
    } else if (tag == '#comment') {
        datum.comment = element.text();
    }
    return datum;
}

function extractAllElementsData(element) {

    var datum = extractElementData(element);
    datum.children = [];
    var children = d3.selectAll(element.node().childNodes);
    children.each(function () {
        var childData = extractAllElementsData(d3.select(this));
        childData.parent = datum;
        datum.children.push(childData);
    });
    return datum;
}

function createElement(data) {

    if (data.tag == '#text') {
        return document.createTextNode("");
    } else if (data.tag == '#comment') {
        return document.createComment(data.comment);
    } else {
        return document.createElementNS('http://www.w3.org/2000/svg', data.tag);
    }
}

function createElementWithAttributes(data) {

    var elementNode = createElement(data);
    var element = d3.select(elementNode);
    var attributes = data.attributes;
    var _iteratorNormalCompletion = true;
    var _didIteratorError = false;
    var _iteratorError = undefined;

    try {
        for (var _iterator = Object.keys(attributes)[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
            var attributeName = _step.value;

            var attributeValue = attributes[attributeName];
            element.attr(attributeName, attributeValue);
        }
    } catch (err) {
        _didIteratorError = true;
        _iteratorError = err;
    } finally {
        try {
            if (!_iteratorNormalCompletion && _iterator.return) {
                _iterator.return();
            }
        } finally {
            if (_didIteratorError) {
                throw _iteratorError;
            }
        }
    }

    return elementNode;
}

function replaceElement(element, data) {
    var parent = d3.select(element.node().parentNode);
    var newElementNode = createElementWithAttributes(data);
    var newElement = parent.insert(function () {
        return newElementNode;
    }, function () {
        return element.node();
    });
    element.remove();
    return newElement;
}

function insertElementData(element, datum) {
    element.datum(datum);
    element.data([datum], function (d) {
        return d.key;
    });
}

function insertAllElementsData(element, datum) {
    insertElementData(element, datum);
    var children = d3.selectAll(element.node().childNodes);
    children.each(function (d, i) {
        insertAllElementsData(d3.select(this), datum.children[i]);
    });
}

function shallowCopyObject(obj) {
    return Object.assign({}, obj);
}

function roundTo4Decimals(x) {
    return Math.round(x * 10000.0) / 10000.0;
}

var zoom$1 = function (enable) {

    this._options.zoom = enable;

    if (this._options.zoom && !this._zoomBehavior) {
        createZoomBehavior.call(this);
    }

    return this;
};

function createZoomBehavior() {

    function zoomed() {
        var g = d3.select(svg.node().querySelector("g"));
        g.attr('transform', d3.event.transform);
    }

    var root = this._selection;
    var svg = d3.select(root.node().querySelector("svg"));
    if (svg.size() == 0) {
        return this;
    }
    this._zoomSelection = svg;
    var zoomBehavior = d3Zoom.zoom().scaleExtent(this._options.zoomScaleExtent).translateExtent(this._options.zoomTranslateExtent).interpolate(d3Interpolate.interpolate).on("zoom", zoomed);
    this._zoomBehavior = zoomBehavior;
    var g = d3.select(svg.node().querySelector("g"));
    svg.call(zoomBehavior);
    if (!this._active) {
        translateZoomBehaviorTransform.call(this, g);
    }
    this._originalTransform = d3Zoom.zoomTransform(svg.node());

    return this;
}

function getTranslatedZoomTransform(selection$$1) {

    // Get the current zoom transform for the top level svg and
    // translate it uniformly with the given selection, using the
    // difference between the translation specified in the selection's
    // data and it's saved previous translation. The selection is
    // normally the top level g element of the graph.
    var oldTranslation = this._translation;
    var oldScale = this._scale;
    var newTranslation = selection$$1.datum().translation;
    var newScale = selection$$1.datum().scale;
    var t = d3Zoom.zoomTransform(this._zoomSelection.node());
    if (oldTranslation) {
        t = t.scale(1 / oldScale);
        t = t.translate(-oldTranslation.x, -oldTranslation.y);
    }
    t = t.translate(newTranslation.x, newTranslation.y);
    t = t.scale(newScale);
    return t;
}

function translateZoomBehaviorTransform(selection$$1) {

    // Translate the current zoom transform for the top level svg
    // uniformly with the given selection, using the difference
    // between the translation specified in the selection's data and
    // it's saved previous translation. The selection is normally the
    // top level g element of the graph.
    this._zoomBehavior.transform(this._zoomSelection, getTranslatedZoomTransform.call(this, selection$$1));

    // Save the selections's new translation and scale.
    this._translation = selection$$1.datum().translation;
    this._scale = selection$$1.datum().scale;

    // Set the original zoom transform to the translation and scale specified in
    // the selection's data.
    this._originalTransform = d3Zoom.zoomIdentity.translate(selection$$1.datum().translation.x, selection$$1.datum().translation.y).scale(selection$$1.datum().scale);
}

function resetZoom(transition$$1) {

    // Reset the zoom transform to the original zoom transform.
    var selection$$1 = this._zoomSelection;
    if (transition$$1) {
        selection$$1 = selection$$1.transition(transition$$1);
    }
    selection$$1.call(this._zoomBehavior.transform, this._originalTransform);

    return this;
}

function zoomScaleExtent(extent) {

    this._options.zoomScaleExtent = extent;

    return this;
}

function zoomTranslateExtent(extent) {

    this._options.zoomTranslateExtent = extent;

    return this;
}

function pathTween(points, d1) {
    return function () {
        var pointInterpolators = points.map(function (p) {
            return d3Interpolate.interpolate([p[0][0], p[0][1]], [p[1][0], p[1][1]]);
        });
        return function (t) {
            return t < 1 ? "M" + pointInterpolators.map(function (p) {
                return p(t);
            }).join("L") : d1;
        };
    };
}

function pathTweenPoints(node, d1, precision) {
    var path0 = node;
    var path1 = path0.cloneNode();
    var n0 = path0.getTotalLength();
    var n1 = (path1.setAttribute("d", d1), path1).getTotalLength();

    // Uniform sampling of distance based on specified precision.
    var distances = [0],
        i = 0,
        dt = precision / Math.max(n0, n1);
    while ((i += dt) < 1) {
        distances.push(i);
    }distances.push(1);

    // Compute point-interpolators at each distance.
    var points = distances.map(function (t) {
        var p0 = path0.getPointAtLength(t * n0);
        var p1 = path1.getPointAtLength(t * n1);
        return [[p0.x, p0.y], [p1.x, p1.y]];
    });
    return points;
}

function isEdgeElementParent(datum) {
    return datum.attributes.class == 'edge' || datum.tag == 'a' && datum.parent.tag == 'g' && datum.parent.parent.attributes.class == 'edge';
}

function isEdgeElement(datum) {
    return datum.parent && isEdgeElementParent(datum.parent);
}

function getEdgeGroup(datum) {
    if (datum.parent.attributes.class == 'edge') {
        return datum.parent;
    } else {
        // datum.parent.tag == 'g' && datum.parent.parent.tag == 'g' && datum.parent.parent.parent.attributes.class == 'edge'
        return datum.parent.parent.parent;
    }
}

function getEdgeTitle(datum) {
    return getEdgeGroup(datum).children.find(function (e) {
        return e.tag == 'title';
    });
}

var render = function (callback) {

    if (this._busy) {
        this._queue.push(this.render.bind(this, callback));
        return this;
    }
    this._dispatch.call('renderStart', this);

    if (this._transitionFactory) {
        d3Timer.timeout(function () {
            // Decouple from time spent. See https://github.com/d3/d3-timer/issues/27
            this._transition = d3Transition.transition(this._transitionFactory());
            _render.call(this, callback);
        }.bind(this), 0);
    } else {
        _render.call(this, callback);
    }
    return this;
};

function _render(callback) {

    var transitionInstance = this._transition;
    var fade = this._options.fade && transitionInstance != null;
    var tweenPaths = this._options.tweenPaths;
    var tweenShapes = this._options.tweenShapes;
    var convertEqualSidedPolygons = this._options.convertEqualSidedPolygons;
    var tweenPrecision = this._options.tweenPrecision;
    var growEnteringEdges = this._options.growEnteringEdges && transitionInstance != null;
    var attributer = this._attributer;
    var graphvizInstance = this;

    function insertChildren(element) {
        var children = element.selectAll(function () {
            return element.node().childNodes;
        });

        children = children.data(function (d) {
            return d.children;
        }, function (d) {
            return d.key;
        });
        var childrenEnter = children.enter().append(function (d) {
            var element = createElement(d);
            if (d.tag == '#text' && fade) {
                element.nodeValue = d.text;
            }
            return element;
        });

        if (fade || growEnteringEdges && isEdgeElementParent(element.datum())) {
            var childElementsEnter = childrenEnter.filter(function (d) {
                return d.tag[0] == '#' ? null : this;
            }).each(function (d) {
                var childEnter = d3.select(this);
                var _iteratorNormalCompletion = true;
                var _didIteratorError = false;
                var _iteratorError = undefined;

                try {
                    for (var _iterator = Object.keys(d.attributes)[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
                        var attributeName = _step.value;

                        var attributeValue = d.attributes[attributeName];
                        childEnter.attr(attributeName, attributeValue);
                    }
                } catch (err) {
                    _didIteratorError = true;
                    _iteratorError = err;
                } finally {
                    try {
                        if (!_iteratorNormalCompletion && _iterator.return) {
                            _iterator.return();
                        }
                    } finally {
                        if (_didIteratorError) {
                            throw _iteratorError;
                        }
                    }
                }
            });
            childElementsEnter.filter(function (d) {
                return d.tag == 'svg' || d.tag == 'g' ? null : this;
            }).style("opacity", 0.0);
        }
        var childrenExit = children.exit();
        if (attributer) {
            childrenExit.each(attributer);
        }
        if (transitionInstance) {
            childrenExit = childrenExit.transition(transitionInstance);
            if (fade) {
                childrenExit.filter(function (d) {
                    return d.tag[0] == '#' ? null : this;
                }).style("opacity", 0.0);
            }
        }
        childrenExit = childrenExit.remove();
        children = childrenEnter.merge(children);
        children.each(attributeElement);
    }

    function attributeElement(data) {
        var element = d3.select(this);
        if (data.tag == "svg") {
            var options = graphvizInstance._options;
            if (options.width != null || options.height != null) {
                var width = options.width;
                var height = options.height;
                if (width == null) {
                    width = data.attributes.width.replace('pt', '') * 4 / 3;
                } else {
                    element.attr("width", width);
                    data.attributes.width = width;
                }
                if (height == null) {
                    height = data.attributes.height.replace('pt', '') * 4 / 3;
                } else {
                    element.attr("height", height);
                    data.attributes.height = height;
                }
                if (!options.fit) {
                    element.attr("viewBox", "0 0 " + width * 3 / 4 / options.scale + " " + height * 3 / 4 / options.scale);
                    data.attributes.viewBox = "0 0 " + width * 3 / 4 / options.scale + " " + height * 3 / 4 / options.scale;
                }
            }
            if (options.scale != 1 && (options.fit || options.width == null && options.height == null)) {
                width = data.attributes.viewBox.split(' ')[2];
                height = data.attributes.viewBox.split(' ')[3];
                element.attr("viewBox", "0 0 " + width / options.scale + " " + height / options.scale);
                data.attributes.viewBox = "0 0 " + width / options.scale + " " + height / options.scale;
            }
        }
        if (attributer) {
            element.each(attributer);
        }
        var tag = data.tag;
        var attributes = data.attributes;
        var currentAttributes = element.node().attributes;
        if (currentAttributes) {
            for (var i = 0; i < currentAttributes.length; i++) {
                var currentAttribute = currentAttributes[i];
                var name = currentAttribute.name;
                if (name.split(':')[0] != 'xmlns' && currentAttribute.namespaceURI) {
                    var namespaceURIParts = currentAttribute.namespaceURI.split('/');
                    var namespace = namespaceURIParts[namespaceURIParts.length - 1];
                    name = namespace + ':' + name;
                }
                if (!(name in attributes)) {
                    attributes[name] = null;
                }
            }
        }
        var convertShape = false;
        var convertPrevShape = false;
        if (tweenShapes && transitionInstance) {
            if ((this.nodeName == 'polygon' || this.nodeName == 'ellipse') && data.alternativeOld) {
                convertPrevShape = true;
            }
            if ((tag == 'polygon' || tag == 'ellipse') && data.alternativeNew) {
                convertShape = true;
            }
            if (this.nodeName == 'polygon' && tag == 'polygon') {
                var prevData = extractElementData(element);
                var prevPoints = prevData.attributes.points;
                if (!convertEqualSidedPolygons) {
                    var nPrevPoints = prevPoints.split(' ').length;
                    var points = data.attributes.points;
                    var nPoints = points.split(' ').length;
                    if (nPoints == nPrevPoints) {
                        convertShape = false;
                        convertPrevShape = false;
                    }
                }
            }
            if (convertPrevShape) {
                var prevPathData = data.alternativeOld;
                var pathElement = replaceElement(element, prevPathData);
                pathElement.data([data], function () {
                    return data.key;
                });
                element = pathElement;
            }
            if (convertShape) {
                var newPathData = data.alternativeNew;
                tag = 'path';
                attributes = newPathData.attributes;
            }
        }
        var elementTransition = element;
        if (transitionInstance) {
            elementTransition = elementTransition.transition(transitionInstance);
            if (fade) {
                elementTransition.filter(function (d) {
                    return d.tag[0] == '#' ? null : this;
                }).style("opacity", 1.0);
            }
            elementTransition.filter(function (d) {
                return d.tag[0] == '#' ? null : this;
            }).on("end", function () {
                d3.select(this).attr('style', null);
            });
        }
        var growThisPath = growEnteringEdges && tag == 'path' && data.offset;
        if (growThisPath) {
            var totalLength = data.totalLength;
            element.attr("stroke-dasharray", totalLength + " " + totalLength).attr("stroke-dashoffset", totalLength).attr('transform', 'translate(' + data.offset.x + ',' + data.offset.y + ')');
            attributes["stroke-dashoffset"] = 0;
            attributes['transform'] = 'translate(0,0)';
            elementTransition.attr("stroke-dashoffset", attributes["stroke-dashoffset"]).attr('transform', attributes['transform']).on("start", function () {
                d3.select(this).style('opacity', null);
            }).on("end", function () {
                d3.select(this).attr('stroke-dashoffset', null).attr('stroke-dasharray', null).attr('transform', null);
            });
        }
        var moveThisPolygon = growEnteringEdges && tag == 'polygon' && isEdgeElement(data) && data.offset;
        if (moveThisPolygon) {
            var edgePath = d3.select(element.node().parentNode.querySelector("path"));
            var p0 = edgePath.node().getPointAtLength(0);
            var p1 = edgePath.node().getPointAtLength(data.totalLength);
            var p2 = edgePath.node().getPointAtLength(data.totalLength - 1);
            var angle1 = Math.atan2(p1.y - p2.y, p1.x - p2.x) * 180 / Math.PI;
            var x = p0.x - p1.x + data.offset.x;
            var y = p0.y - p1.y + data.offset.y;
            element.attr('transform', 'translate(' + x + ',' + y + ')');
            elementTransition.attrTween("transform", function () {
                return function (t) {
                    var p = edgePath.node().getPointAtLength(data.totalLength * t);
                    var p2 = edgePath.node().getPointAtLength(data.totalLength * t + 1);
                    var angle = Math.atan2(p2.y - p.y, p2.x - p.x) * 180 / Math.PI - angle1;
                    x = p.x - p1.x + data.offset.x * (1 - t);
                    y = p.y - p1.y + data.offset.y * (1 - t);
                    return 'translate(' + x + ',' + y + ') rotate(' + angle + ' ' + p1.x + ' ' + p1.y + ')';
                };
            }).on("start", function () {
                d3.select(this).style('opacity', null);
            }).on("end", function () {
                d3.select(this).attr('transform', null);
            });
        }
        var tweenThisPath = tweenPaths && transitionInstance && tag == 'path' && element.attr('d') != null;
        var _iteratorNormalCompletion2 = true;
        var _didIteratorError2 = false;
        var _iteratorError2 = undefined;

        try {
            for (var _iterator2 = Object.keys(attributes)[Symbol.iterator](), _step2; !(_iteratorNormalCompletion2 = (_step2 = _iterator2.next()).done); _iteratorNormalCompletion2 = true) {
                var attributeName = _step2.value;

                var attributeValue = attributes[attributeName];
                if (tweenThisPath && attributeName == 'd') {
                    var points = (data.alternativeOld || data).points;
                    if (points) {
                        elementTransition.attrTween("d", pathTween(points, attributeValue));
                    }
                } else {
                    if (attributeName == 'transform' && data.translation) {
                        if (transitionInstance) {
                            var onEnd = elementTransition.on("end");
                            elementTransition.on("start", function () {
                                if (graphvizInstance._zoomBehavior) {
                                    // Update the transform to transition to, just before the transition starts
                                    // in order to catch changes between the transition scheduling to its start.
                                    elementTransition.tween("attr.transform", function () {
                                        var node = this;
                                        return function (t) {
                                            node.setAttribute("transform", d3Interpolate.interpolateTransformSvg(d3Zoom.zoomTransform(graphvizInstance._zoomSelection.node()).toString(), getTranslatedZoomTransform.call(graphvizInstance, element).toString())(t));
                                        };
                                    });
                                }
                            }).on("end", function () {
                                onEnd.call(this);
                                // Update the zoom transform to the new translated transform
                                if (graphvizInstance._zoomBehavior) {
                                    translateZoomBehaviorTransform.call(graphvizInstance, element);
                                }
                            });
                        } else {
                            if (graphvizInstance._zoomBehavior) {
                                // Update the transform attribute to set with the current pan translation
                                attributeValue = getTranslatedZoomTransform.call(graphvizInstance, element).toString();
                            }
                        }
                    }
                    elementTransition.attr(attributeName, attributeValue);
                }
            }
        } catch (err) {
            _didIteratorError2 = true;
            _iteratorError2 = err;
        } finally {
            try {
                if (!_iteratorNormalCompletion2 && _iterator2.return) {
                    _iterator2.return();
                }
            } finally {
                if (_didIteratorError2) {
                    throw _iteratorError2;
                }
            }
        }

        if (convertShape) {
            elementTransition.on("end", function (d, i, nodes) {
                pathElement = d3.select(this);
                var newElement = replaceElement(pathElement, d);
                newElement.data([d], function () {
                    return d.key;
                });
            });
        }
        if (data.text) {
            elementTransition.text(data.text);
        }
        insertChildren(element);
    }

    var root = this._selection;

    if (transitionInstance != null) {
        // Ensure original SVG shape elements are restored after transition before rendering new graph
        var jobs = this._jobs;
        if (graphvizInstance._active) {
            jobs.push(null);
            return this;
        } else {
            root.transition(transitionInstance).transition().duration(0).on("end", function () {
                graphvizInstance._active = false;
                if (jobs.length != 0) {
                    jobs.shift();
                    graphvizInstance.render();
                }
            });
            this._active = true;
        }
    }

    if (transitionInstance != null) {
        root.transition(transitionInstance).on("start", function () {
            graphvizInstance._dispatch.call('transitionStart', graphvizInstance);
        }).on("end", function () {
            graphvizInstance._dispatch.call('transitionEnd', graphvizInstance);
        }).transition().duration(0).on("start", function () {
            graphvizInstance._dispatch.call('restoreEnd', graphvizInstance);
            graphvizInstance._dispatch.call('end', graphvizInstance);
            if (callback) {
                callback.call(graphvizInstance);
            }
        });
    }

    var data = this._data;

    var svg = root.selectAll("svg").data([data], function (d) {
        return d.key;
    });
    svg = svg.enter().append("svg").merge(svg);

    attributeElement.call(svg.node(), data);

    if (this._options.zoom && !this._zoomBehavior) {
        createZoomBehavior.call(this);
    }

    graphvizInstance._dispatch.call('renderEnd', graphvizInstance);

    if (transitionInstance == null) {
        this._dispatch.call('end', this);
        if (callback) {
            callback.call(this);
        }
    }

    return this;
}

function convertToPathData(originalData, guideData) {
    if (originalData.tag == 'polygon') {
        var newData = shallowCopyObject(originalData);
        newData.tag = 'path';
        var originalAttributes = originalData.attributes;
        var newAttributes = shallowCopyObject(originalAttributes);
        var newPointsString = originalAttributes.points;
        if (guideData.tag == 'polygon') {
            var bbox = originalData.bbox;
            bbox.cx = bbox.x + bbox.width / 2;
            bbox.cy = bbox.y + bbox.height / 2;
            var pointsString = originalAttributes.points;
            var pointStrings = pointsString.split(' ');
            var normPoints = pointStrings.map(function (p) {
                var xy = p.split(',');return [xy[0] - bbox.cx, xy[1] - bbox.cy];
            });
            var x0 = normPoints[normPoints.length - 1][0];
            var y0 = normPoints[normPoints.length - 1][1];
            for (var i = 0; i < normPoints.length; i++, x0 = x1, y0 = y1) {
                var x1 = normPoints[i][0];
                var y1 = normPoints[i][1];
                var dx = x1 - x0;
                var dy = y1 - y0;
                if (dy == 0) {
                    continue;
                } else {
                    var x2 = x0 - y0 * dx / dy;
                }
                if (0 <= x2 && x2 < Infinity && (x0 <= x2 && x2 <= x1 || x1 <= x2 && x2 <= x0)) {
                    break;
                }
            }
            var newPointStrings = [[bbox.cx + x2, bbox.cy + 0].join(',')];
            newPointStrings = newPointStrings.concat(pointStrings.slice(i));
            newPointStrings = newPointStrings.concat(pointStrings.slice(0, i));
            newPointsString = newPointStrings.join(' ');
        }
        newAttributes['d'] = 'M' + newPointsString + 'z';
        delete newAttributes.points;
        newData.attributes = newAttributes;
    } else /* if (originalData.tag == 'ellipse') */{
            var newData = shallowCopyObject(originalData);
            newData.tag = 'path';
            var originalAttributes = originalData.attributes;
            var newAttributes = shallowCopyObject(originalAttributes);
            var cx = originalAttributes.cx;
            var cy = originalAttributes.cy;
            var rx = originalAttributes.rx;
            var ry = originalAttributes.ry;
            if (guideData.tag == 'polygon') {
                var bbox = guideData.bbox;
                bbox.cx = bbox.x + bbox.width / 2;
                bbox.cy = bbox.y + bbox.height / 2;
                var p = guideData.attributes.points.split(' ')[0].split(',');
                var sx = p[0];
                var sy = p[1];
                var dx = sx - bbox.cx;
                var dy = sy - bbox.cy;
                var l = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
                var cosA = dx / l;
                var sinA = -dy / l;
            } else {
                // if (guideData.tag == 'path') {
                // FIXME: add support for getting start position from path
                var cosA = 1;
                var sinA = 0;
            }
            var x1 = rx * cosA;
            var y1 = -ry * sinA;
            var x2 = rx * -cosA;
            var y2 = -ry * -sinA;
            var dx = x2 - x1;
            var dy = y2 - y1;
            newAttributes['d'] = 'M ' + cx + ' ' + cy + ' m ' + x1 + ',' + y1 + ' a ' + rx + ',' + ry + ' 0 1,0 ' + dx + ',' + dy + ' a ' + rx + ',' + ry + ' 0 1,0 ' + -dx + ',' + -dy + 'z';
            delete newAttributes.cx;
            delete newAttributes.cy;
            delete newAttributes.rx;
            delete newAttributes.ry;
            newData.attributes = newAttributes;
        }
    return newData;
}

function translatePointsAttribute(pointsString, x, y) {
    var pointStrings = pointsString.split(' ');
    var points = pointStrings.map(function (p) {
        return p.split(',');
    });
    var points = pointStrings.map(function (p) {
        return [roundTo4Decimals(+x + +p.split(',')[0]), roundTo4Decimals(+y + +p.split(',')[1])];
    });
    var pointStrings = points.map(function (p) {
        return p.join(',');
    });
    var pointsString = pointStrings.join(' ');
    pointsString = pointsString.replace(/-0\./g, '-.').replace(/ 0\./g, ' .');
    return pointsString;
}

function translateDAttribute(d, x, y) {
    var pointStrings = d.split(/[A-Z ]/);
    pointStrings.shift();
    var commands = d.split(/[^[A-Z ]+/);
    var points = pointStrings.map(function (p) {
        return p.split(',');
    });
    var points = pointStrings.map(function (p) {
        return [roundTo4Decimals(+x + +p.split(',')[0]), roundTo4Decimals(+y + +p.split(',')[1])];
    });
    var pointStrings = points.map(function (p) {
        return p.join(',');
    });
    d = commands.reduce(function (arr, v, i) {
        return arr.concat(v, pointStrings[i]);
    }, []).join('');
    d = d.replace(/-0\./g, '-.').replace(/ 0\./g, ' .');
    return d;
}

function initViz() {
    // force JIT compilation of Viz.js
    if (this._worker == null) {
        Viz("");
        this._dispatch.call("initEnd", this);
    } else {
        var vizURL = this._vizURL;
        var graphvizInstance = this;
        this._worker.onmessage = function (event$$1) {
            graphvizInstance._dispatch.call("initEnd", this);
        };
        if (!vizURL.match(/^https?:\/\/|^\/\//i)) {
            // Local URL. Prepend with local domain to be usable in web worker
            vizURL = new window.URL(vizURL, document.location.href).href;
        }
        this._worker.postMessage({ dot: "", vizURL: vizURL });
    }
}

var dot = function (src, callback) {

    var graphvizInstance = this;
    var worker = this._worker;
    var engine = this._options.engine;
    var images = this._images;
    var totalMemory = this._options.totalMemory;
    var keyMode = this._options.keyMode;
    var tweenPaths = this._options.tweenPaths;
    var tweenShapes = this._options.tweenShapes;
    var tweenPrecision = this._options.tweenPrecision;
    var growEnteringEdges = this._options.growEnteringEdges;
    var dictionary = {};
    var prevDictionary = this._dictionary || {};
    var nodeDictionary = {};
    var prevNodeDictionary = this._nodeDictionary || {};

    function setKey(datum, index) {
        var tag = datum.tag;
        if (keyMode == 'index') {
            datum.key = index;
        } else if (tag[0] != '#') {
            if (keyMode == 'id') {
                datum.key = datum.attributes.id;
            } else if (keyMode == 'title') {
                var title = datum.children.find(function (childData) {
                    return childData.tag == 'title';
                });
                if (title) {
                    datum.key = title.children[0].text;
                }
            }
        }
        if (datum.key == null) {
            if (tweenShapes) {
                if (tag == 'ellipse' || tag == 'polygon') {
                    tag = 'path';
                }
            }
            datum.key = tag + '-' + index;
        }
    }

    function setId(datum, parentData) {
        var id = (parentData ? parentData.id + '.' : '') + datum.key;
        datum.id = id;
    }

    function addToDictionary(datum) {
        dictionary[datum.id] = datum;
    }

    function calculateAlternativeShapeData(datum, prevDatum) {
        if (tweenShapes && datum.id in prevDictionary) {
            if ((prevDatum.tag == 'polygon' || prevDatum.tag == 'ellipse' || prevDatum.tag == 'path') && (prevDatum.tag != datum.tag || datum.tag == 'polygon')) {
                if (prevDatum.tag != 'path') {
                    datum.alternativeOld = convertToPathData(prevDatum, datum);
                }
                if (datum.tag != 'path') {
                    datum.alternativeNew = convertToPathData(datum, prevDatum);
                }
            }
        }
    }

    function calculatePathTweenPoints(datum, prevDatum) {
        if (tweenPaths && prevDatum && (prevDatum.tag == 'path' || datum.alternativeOld && datum.alternativeOld.tag == 'path')) {
            var attribute_d = (datum.alternativeNew || datum).attributes.d;
            if (datum.alternativeOld) {
                var oldNode = createElementWithAttributes(datum.alternativeOld);
            } else {
                var oldNode = createElementWithAttributes(prevDatum);
            }
            (datum.alternativeOld || (datum.alternativeOld = {})).points = pathTweenPoints(oldNode, attribute_d, tweenPrecision);
        }
    }

    function postProcessDataPass1Local(datum) {
        var index = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : 0;
        var parentData = arguments[2];

        setKey(datum, index);
        setId(datum, parentData);
        var id = datum.id;
        var prevDatum = prevDictionary[id];
        addToDictionary(datum);
        calculateAlternativeShapeData(datum, prevDatum);
        calculatePathTweenPoints(datum, prevDatum);
        var childTagIndexes = {};
        datum.children.forEach(function (childData) {
            var childTag = childData.tag;
            if (childTag == 'ellipse' || childTag == 'polygon') {
                childTag = 'path';
            }
            if (childTagIndexes[childTag] == null) {
                childTagIndexes[childTag] = 0;
            }
            var childIndex = childTagIndexes[childTag]++;
            postProcessDataPass1Local(childData, childIndex, datum);
        });
    }

    function addToNodeDictionary(datum) {
        var tag = datum.tag;
        if (growEnteringEdges && datum.parent) {
            if (datum.parent.attributes.class == 'node') {
                if (tag == 'title') {
                    var child = datum.children[0];
                    var nodeId = child.text;
                    nodeDictionary[nodeId] = datum.parent;
                }
            }
        }
    }

    function extractGrowingEdgesData(datum) {
        var id = datum.id;
        var tag = datum.tag;
        var prevDatum = prevDictionary[id];
        if (growEnteringEdges && !prevDatum && datum.parent) {
            if (isEdgeElement(datum)) {
                if (tag == 'path' || tag == 'polygon') {
                    if (tag == 'polygon') {
                        var path$$1 = datum.parent.children.find(function (e) {
                            return e.tag == 'path';
                        });
                        datum.totalLength = path$$1.totalLength;
                    }
                    var title = getEdgeTitle(datum);
                    var child = title.children[0];
                    var nodeIds = child.text.split('->');
                    if (nodeIds.length != 2) {
                        nodeIds = child.text.split('--');
                    }
                    var startNodeId = nodeIds[0];
                    var startNode = nodeDictionary[startNodeId];
                    var prevStartNode = prevNodeDictionary[startNodeId];
                    if (prevStartNode) {
                        var i = startNode.children.findIndex(function (element, index) {
                            return element.tag == 'g';
                        });
                        if (i >= 0) {
                            var j = startNode.children[i].children.findIndex(function (element, index) {
                                return element.tag == 'a';
                            });
                            startNode = startNode.children[i].children[j];
                        }
                        var i = prevStartNode.children.findIndex(function (element, index) {
                            return element.tag == 'g';
                        });
                        if (i >= 0) {
                            var j = prevStartNode.children[i].children.findIndex(function (element, index) {
                                return element.tag == 'a';
                            });
                            prevStartNode = prevStartNode.children[i].children[j];
                        }
                        var startShapes = startNode.children;
                        for (var i = 0; i < startShapes.length; i++) {
                            if (startShapes[i].tag == 'polygon' || startShapes[i].tag == 'ellipse' || startShapes[i].tag == 'path') {
                                var startShape = startShapes[i];
                                break;
                            }
                        }
                        var prevStartShapes = prevStartNode.children;
                        for (var i = 0; i < prevStartShapes.length; i++) {
                            if (prevStartShapes[i].tag == 'polygon' || prevStartShapes[i].tag == 'ellipse' || prevStartShapes[i].tag == 'path') {
                                var prevStartShape = prevStartShapes[i];
                                break;
                            }
                        }
                        datum.offset = {
                            x: prevStartShape.center.x - startShape.center.x,
                            y: prevStartShape.center.y - startShape.center.y
                        };
                    }
                }
            }
        }
    }

    function postProcessDataPass2Global(datum) {
        addToNodeDictionary(datum);
        extractGrowingEdgesData(datum);
        datum.children.forEach(function (childData) {
            postProcessDataPass2Global(childData);
        });
    }

    this._dispatch.call("start", this);
    this._busy = true;
    this._dispatch.call("layoutStart", this);
    var vizOptions = {
        format: "svg",
        engine: engine,
        images: images,
        totalMemory: totalMemory
    };
    if (this._worker) {
        worker.postMessage({
            dot: src,
            options: vizOptions
        });

        worker.onmessage = function (event$$1) {
            switch (event$$1.data.type) {
                case "done":
                    return layoutDone.call(graphvizInstance, event$$1.data.svg);
                case "error":
                    if (graphvizInstance._onerror) {
                        graphvizInstance._onerror(event$$1.data.error);
                    } else {
                        throw event$$1.data.error;
                    }
                    break;
            }
        };
    } else {
        try {
            var svgDoc = Viz(src, vizOptions);
        } catch (error) {
            if (graphvizInstance._onerror) {
                graphvizInstance._onerror(error.message);
                return this;
            } else {
                throw error.message;
            }
        }
        layoutDone.call(this, svgDoc);
    }

    function layoutDone(svgDoc) {
        this._dispatch.call("layoutEnd", this);

        var newDoc = d3.select(document.createDocumentFragment()).append('div');

        var parser = new window.DOMParser();
        var doc = parser.parseFromString(svgDoc, "image/svg+xml");

        newDoc.append(function () {
            return doc.documentElement;
        });

        var newSvg = newDoc.select('svg');

        var data = extractAllElementsData(newSvg);
        this._dispatch.call('dataExtractEnd', this);
        postProcessDataPass1Local(data);
        this._dispatch.call('dataProcessPass1End', this);
        postProcessDataPass2Global(data);
        this._dispatch.call('dataProcessPass2End', this);
        this._data = data;
        this._dictionary = dictionary;
        this._nodeDictionary = nodeDictionary;

        this._extractData = function (element, childIndex, parentData) {
            var data = extractAllElementsData(element);
            postProcessDataPass1Local(data, childIndex, parentData);
            postProcessDataPass2Global(data);
            return data;
        };
        this._busy = false;
        this._dispatch.call('dataProcessEnd', this);
        if (callback) {
            callback.call(this);
        }
        if (this._queue.length > 0) {
            var job = this._queue.shift();
            job.call(this);
        }
    }

    return this;
};

var renderDot = function (src, callback) {

    var graphvizInstance = this;

    this.dot(src, render);

    function render() {
        graphvizInstance.render(callback);
    }

    return this;
};

var transition$1 = function (name) {

    if (name instanceof Function) {
        this._transitionFactory = name;
    } else {
        this._transition = d3Transition.transition(name);
    }

    return this;
};

function active$1(name) {

    var root = this._selection;
    var svg = root.selectWithoutDataPropagation("svg");
    if (svg.size() != 0) {
        return d3Transition.active(svg.node(), name);
    } else {
        return null;
    }
}

var options = function (options) {

    if (typeof options == 'undefined') {
        return Object.assign({}, this._options);
    } else {
        var _iteratorNormalCompletion = true;
        var _didIteratorError = false;
        var _iteratorError = undefined;

        try {
            for (var _iterator = Object.keys(options)[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
                var option = _step.value;

                this._options[option] = options[option];
            }
        } catch (err) {
            _didIteratorError = true;
            _iteratorError = err;
        } finally {
            try {
                if (!_iteratorNormalCompletion && _iterator.return) {
                    _iterator.return();
                }
            } finally {
                if (_didIteratorError) {
                    throw _iteratorError;
                }
            }
        }

        return this;
    }
};

var width = function (width) {

    this._options.width = width;

    return this;
};

var height = function (height) {

    this._options.height = height;

    return this;
};

var scale = function (scale) {

    this._options.scale = scale;

    return this;
};

var fit = function (fit) {

    this._options.fit = fit;

    return this;
};

var attributer = function (callback) {

    this._attributer = callback;

    return this;
};

var engine = function (engine) {

    this._options.engine = engine;

    return this;
};

var images = function (path$$1, width, height) {

    this._images.push({ path: path$$1, width: width, height: height });

    return this;
};

var totalMemory = function (size) {

    this._options.totalMemory = size;

    return this;
};

var keyMode = function (keyMode) {

    if (!this._keyModes.has(keyMode)) {
        throw Error('Illegal keyMode: ' + keyMode);
    }
    if (keyMode != this._options.keyMode && this._data != null) {
        throw Error('Too late to change keyMode');
    }
    this._options.keyMode = keyMode;

    return this;
};

var fade = function (enable) {

    this._options.fade = enable;

    return this;
};

var tweenPaths = function (enable) {

    this._options.tweenPaths = enable;

    return this;
};

var tweenShapes = function (enable) {

    this._options.tweenShapes = enable;
    if (enable) {
        this._options.tweenPaths = true;
    }

    return this;
};

var convertEqualSidedPolygons = function (enable) {

    this._options.convertEqualSidedPolygons = enable;

    return this;
};

var tweenPrecision = function (precision) {

    this._options.tweenPrecision = precision;

    return this;
};

var growEnteringEdges = function (enable) {

    this._options.growEnteringEdges = enable;

    return this;
};

var on = function (typenames, callback) {

    this._dispatch.on(typenames, callback);

    return this;
};

var onerror = function (callback) {

    this._onerror = callback;

    return this;
};

var defineProperty = function (obj, key, value) {
  if (key in obj) {
    Object.defineProperty(obj, key, {
      value: value,
      enumerable: true,
      configurable: true,
      writable: true
    });
  } else {
    obj[key] = value;
  }

  return obj;
};



































var toConsumableArray = function (arr) {
  if (Array.isArray(arr)) {
    for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) arr2[i] = arr[i];

    return arr2;
  } else {
    return Array.from(arr);
  }
};

var logEvents = function (enable) {
    var _this = this;

    var t0 = Date.now();
    var times = {};
    var eventTypes = this._eventTypes;
    var maxEventTypeLength = Math.max.apply(Math, toConsumableArray(eventTypes.map(function (eventType) {
        return eventType.length;
    })));

    var _loop = function _loop(i) {
        var eventType = eventTypes[i];
        times[eventType] = [];
        graphvizInstance = _this;

        _this.on(eventType + '.log', enable ? function () {
            var t = Date.now();
            var seqNo = times[eventType].length;
            times[eventType].push(t);
            var string = '';
            string += 'Event ';
            string += d3Format.format(' >2')(i) + ' ';
            string += eventType + ' '.repeat(maxEventTypeLength - eventType.length);
            string += d3Format.format(' >5')(t - t0) + ' ';
            if (eventType != 'initEnd') {
                string += d3Format.format(' >5')(t - times['start'][seqNo]);
            }
            if (eventType == 'dataProcessEnd') {
                string += ' prepare                 ' + d3Format.format(' >5')(t - times['layoutEnd'][seqNo]);
            }
            if (eventType == 'renderEnd' && graphvizInstance._transition) {
                string += ' transition start margin ' + d3Format.format(' >5')(graphvizInstance._transition.delay() - (t - times['renderStart'][seqNo]));
                expectedDelay = graphvizInstance._transition.delay();
                expectedDuration = graphvizInstance._transition.duration();
            }
            if (eventType == 'transitionStart') {
                var actualDelay = t - times['renderStart'][seqNo];
                string += ' transition delay        ' + d3Format.format(' >5')(t - times['renderStart'][seqNo]);
                string += ' expected ' + d3Format.format(' >5')(expectedDelay);
                string += ' diff ' + d3Format.format(' >5')(actualDelay - expectedDelay);
            }
            if (eventType == 'transitionEnd') {
                var actualDuration = t - times['transitionStart'][seqNo];
                string += ' transition duration     ' + d3Format.format(' >5')(actualDuration);
                string += ' expected ' + d3Format.format(' >5')(expectedDuration);
                string += ' diff ' + d3Format.format(' >5')(actualDuration - expectedDuration);
            }
            console.log(string);
            t0 = t;
        } : null);
    };

    for (var i in eventTypes) {
        var graphvizInstance;
        var expectedDelay;
        var expectedDuration;

        _loop(i);
    }
    return this;
};

function rotate(x, y, cosA, sinA) {
    // (x + j * y) * (cosA + j * sinA) = x * cosA - y * sinA + j * (x * sinA + y * cosA)
    y = -y;
    sinA = -sinA;
    var _ref = [x * cosA - y * sinA, x * sinA + y * cosA];
    x = _ref[0];
    y = _ref[1];

    y = -y;
    return [x, y];
}

var defaultEdgeAttributes = {
    id: null,
    fillcolor: "black",
    color: "black",
    penwidth: 1,
    URL: null,
    tooltip: null
};

function completeAttributes(attributes) {
    var defaultAttributes = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : defaultEdgeAttributes;

    for (var attribute in defaultAttributes) {
        if (attributes[attribute] === undefined) {
            attributes[attribute] = defaultAttributes[attribute];
        }
    }
}

function drawEdge(x1, y1, x2, y2, attributes) {
    var options = arguments.length > 5 && arguments[5] !== undefined ? arguments[5] : {};

    attributes = attributes || {};
    completeAttributes(attributes);
    var root = this._selection;
    var svg = root.selectWithoutDataPropagation("svg");
    var graph0 = svg.selectWithoutDataPropagation("g");
    var newEdge = graph0.append("g").datum(null).attr("class", "edge");
    var title = newEdge.insert('title', ':first-child').text("");
    if (attributes.URL || attributes.tooltip) {
        var a = newEdge.append("g").append("a");
        if (attributes.URL) {
            a.attr("href", attributes.URL);
        }
        if (attributes.tooltip) {
            a.attr('title', attributes.tooltip);
        }
        var line = a.append("path");
        var arrowHead = a.append("polygon");
    } else {
        var line = newEdge.append("path");
        var arrowHead = newEdge.append("polygon");
    }
    this._drawnEdge = {
        g: newEdge,
        x1: x1,
        y1: y1,
        x2: x2,
        y2: y2,
        attributes: attributes
    };
    _updateEdge(newEdge, x1, y1, x2, y2, attributes, options);

    return this;
}

function updateDrawnEdge(x1, y1, x2, y2, attributes) {
    var options = arguments.length > 5 && arguments[5] !== undefined ? arguments[5] : {};

    if (!this._drawnEdge) {
        throw Error('No edge has been drawn');
    }
    var edge = this._drawnEdge.g;
    attributes = attributes || {};
    completeAttributes(attributes, this._drawnEdge.attributes);
    this._drawnEdge.x1 = x1;
    this._drawnEdge.y1 = y1;
    this._drawnEdge.x2 = x2;
    this._drawnEdge.y2 = y2;
    this._drawnEdge.attributes = attributes;
    _updateEdge(edge, x1, y1, x2, y2, attributes, options);

    return this;
}

function _updateEdge(edge, x1, y1, x2, y2, attributes, options) {

    var id = attributes.id;
    var fill = attributes.fillcolor;
    var stroke = attributes.color;
    var strokeWidth = attributes.penwidth;
    var shortening = options.shortening || 0;
    var arrowHeadLength = 10;
    var arrowHeadWidth = 7;
    var margin = 0.174;

    var arrowHeadPoints = [[0, -arrowHeadWidth / 2], [arrowHeadLength, 0], [0, arrowHeadWidth / 2], [0, -arrowHeadWidth / 2]];

    var dx = x2 - x1;
    var dy = y2 - y1;
    var length = Math.sqrt(dx * dx + dy * dy);
    var cosA = dx / length;
    var sinA = dy / length;
    x2 = x1 + (length - shortening - arrowHeadLength - margin) * cosA;
    y2 = y1 + (length - shortening - arrowHeadLength - margin) * sinA;

    if (attributes.URL || attributes.tooltip) {
        var a = edge.selectWithoutDataPropagation("g").selectWithoutDataPropagation("a");
        var line = a.selectWithoutDataPropagation("path");
        var arrowHead = a.selectWithoutDataPropagation("polygon");
    } else {
        var line = edge.selectWithoutDataPropagation("path");
        var arrowHead = edge.selectWithoutDataPropagation("polygon");
    }

    edge.attr("id", id);

    var path1 = d3Path.path();
    path1.moveTo(x1, y1);
    path1.lineTo(x2, y2);

    line.attr("d", path1).attr("fill", fill).attr("stroke", stroke).attr("strokeWidth", strokeWidth);

    x2 = x1 + (length - shortening - arrowHeadLength) * cosA;
    y2 = y1 + (length - shortening - arrowHeadLength) * sinA;
    for (var i = 0; i < arrowHeadPoints.length; i++) {
        var point = arrowHeadPoints[i];
        arrowHeadPoints[i] = rotate(point[0], point[1], cosA, sinA);
    }
    for (var i = 0; i < arrowHeadPoints.length; i++) {
        var point = arrowHeadPoints[i];
        arrowHeadPoints[i] = [x2 + point[0], y2 + point[1]];
    }
    var allPoints = [];
    for (var i = 0; i < arrowHeadPoints.length; i++) {
        var point = arrowHeadPoints[i];
        allPoints.push(point.join(','));
    }
    var pointsAttr = allPoints.join(' ');

    arrowHead.attr("points", pointsAttr).attr("fill", fill).attr("stroke", stroke).attr("strokeWidth", strokeWidth);

    return this;
}

function moveDrawnEdgeEndPoint(x2, y2) {
    var options = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : {};


    if (!this._drawnEdge) {
        throw Error('No edge has been drawn');
    }
    var edge = this._drawnEdge.g;
    var x1 = this._drawnEdge.x1;
    var y1 = this._drawnEdge.y1;
    var attributes = this._drawnEdge.attributes;

    this._drawnEdge.x2 = x2;
    this._drawnEdge.y2 = y2;
    _updateEdge(edge, x1, y1, x2, y2, attributes, options);

    return this;
}

function removeDrawnEdge() {

    if (!this._drawnEdge) {
        return this;
    }

    var edge = this._drawnEdge.g;

    edge.remove();

    this._drawnEdge = null;

    return this;
}

function insertDrawnEdge(name) {

    if (!this._drawnEdge) {
        throw Error('No edge has been drawn');
    }

    var edge = this._drawnEdge.g;
    var attributes = this._drawnEdge.attributes;

    var title = edge.selectWithoutDataPropagation("title");
    title.text(name);
    var text = title.selectAll(function () {
        return title.node().childNodes;
    });
    if (attributes.URL || attributes.tooltip) {
        var ga = edge.selectWithoutDataPropagation("g");
        var a = ga.selectWithoutDataPropagation("a");
        var line = a.selectWithoutDataPropagation("path");
        var arrowHead = a.selectWithoutDataPropagation("polygon");
    } else {
        var line = edge.selectWithoutDataPropagation("path");
        var arrowHead = edge.selectWithoutDataPropagation("polygon");
    }
    var root = this._selection;
    var svg = root.selectWithoutDataPropagation("svg");
    var graph0 = svg.selectWithoutDataPropagation("g");
    var graph0Datum = graph0.datum();
    var edgeData = this._extractData(edge, graph0Datum.children.length, graph0.datum());
    var gDatum = edgeData;
    var titleDatum = gDatum.children[0];
    var textDatum = titleDatum.children[0];
    if (attributes.URL || attributes.tooltip) {
        var gaDatum = gDatum.children[1];
        var aDatum = gaDatum.children[0];
        var pathDatum = aDatum.children[0];
        var polygonDatum = aDatum.children[1];
    } else {
        var pathDatum = gDatum.children[1];
        var polygonDatum = gDatum.children[2];
    }
    graph0Datum.children.push(gDatum);

    edge.datum(gDatum);
    edge.data([gDatum], function (d) {
        return d.key;
    });

    title.datum(titleDatum);
    title.data([titleDatum], function (d) {
        return [d.key];
    });

    text.datum(textDatum);
    text.data([textDatum], function (d) {
        return [d.key];
    });

    if (attributes.URL || attributes.tooltip) {
        ga.datum(gaDatum);
        ga.data([gaDatum], function (d) {
            return [d.key];
        });

        a.datum(aDatum);
        a.data([aDatum], function (d) {
            return [d.key];
        });
    }

    line.datum(pathDatum);
    line.data([pathDatum], function (d) {
        return [d.key];
    });

    arrowHead.datum(polygonDatum);
    arrowHead.data([polygonDatum], function (d) {
        return [d.key];
    });

    this._drawnEdge = null;

    return this;
}

var defaultNodeAttributes = {
    id: null,
    fillcolor: "none",
    color: "#000000",
    penwidth: null,
    URL: null,
    tooltip: null,
    labeljust: null,
    fontname: null,
    fontsize: null,
    fontcolor: null
};

var multiFillShapes = ['fivepoverhang', 'threepoverhang', 'noverhang', 'assembly'];

var plainShapes = ['none', 'plain', 'plaintext'];

function completeAttributes$1(attributes) {
    var defaultAttributes = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : defaultNodeAttributes;

    if (attributes.style == 'filled' && !attributes.fillcolor) {
        if (attributes.color) {
            attributes.fillcolor = attributes.color;
        } else {
            attributes.fillcolor = '#d3d3d3';
        }
    }
    if (attributes.style == 'filled') {
        if (plainShapes.includes(attributes.shape)) {
            attributes.color = 'transparent';
        } else if (!attributes.color) {
            attributes.color = '#000000';
        }
    }
    if (attributes.shape == 'point' && !attributes.fillcolor) {
        attributes.fillcolor = '#000000';
    }
    for (var attribute in defaultAttributes) {
        if (attributes[attribute] === undefined) {
            attributes[attribute] = defaultAttributes[attribute];
        }
    }
}

function drawNode(x, y, nodeId) {
    var attributes = arguments.length > 3 && arguments[3] !== undefined ? arguments[3] : {};
    var options = arguments.length > 4 && arguments[4] !== undefined ? arguments[4] : {};

    completeAttributes$1(attributes);
    var root = this._selection;
    var svg = root.selectWithoutDataPropagation("svg");
    var graph0 = svg.selectWithoutDataPropagation("g");
    var newNode = graph0.append(function () {
        return createNode(nodeId, attributes).node();
    });
    newNode.datum(null);

    this._drawnNode = {
        g: newNode,
        nodeId: nodeId,
        x: x,
        y: y,
        attributes: attributes
    };
    _updateNode(newNode, x, y, nodeId, attributes, options);

    return this;
}

function updateDrawnNode(x, y, nodeId) {
    var attributes = arguments.length > 3 && arguments[3] !== undefined ? arguments[3] : {};
    var options = arguments.length > 4 && arguments[4] !== undefined ? arguments[4] : {};

    if (!this._drawnNode) {
        throw Error('No node has been drawn');
    }

    var node = this._drawnNode.g;
    if (nodeId == null) {
        nodeId = this._drawnNode.nodeId;
    }
    completeAttributes$1(attributes, this._drawnNode.attributes);
    this._drawnNode.nodeId = nodeId;
    this._drawnNode.x = x;
    this._drawnNode.y = y;
    this._drawnNode.attributes = attributes;
    _updateNode(node, x, y, nodeId, attributes, options);

    return this;
}

function _updateNode(node, x, y, nodeId, attributes, options) {

    var id = attributes.id;
    var fill = attributes.fillcolor;
    var stroke = attributes.color;
    var strokeWidth = attributes.penwidth;
    if (attributes.labeljust == 'l') {
        var textAnchor = 'start';
    } else if (attributes.labeljust == 'r') {
        var textAnchor = 'end';
    } else if (attributes.labeljust == 'c') {
        var textAnchor = 'middle';
    } else {
        var textAnchor = null;
    }
    var fontFamily = attributes.fontname;
    var fontSize = attributes.fontsize;
    var fontColor = attributes.fontcolor;
    if ('label' in attributes) {
        var label = attributes['label'];
    } else {
        var label = nodeId;
    }

    var title = node.selectWithoutDataPropagation('title');
    if (attributes.URL || attributes.tooltip) {
        var subParent = node.selectWithoutDataPropagation("g").selectWithoutDataPropagation("a");
    } else {
        var subParent = node;
    }
    var svgElements = subParent.selectAll('ellipse,polygon,path,polyline');
    var text = node.selectWithoutDataPropagation("text");

    node.attr("id", id);

    title.text(nodeId);
    if (svgElements.size() != 0) {
        var bbox = svgElements.node().getBBox();
        bbox.cx = bbox.x + bbox.width / 2;
        bbox.cy = bbox.y + bbox.height / 2;
    } else if (text.size() != 0) {
        bbox = {
            x: +text.attr('x'),
            y: +text.attr('y'),
            width: 0,
            height: 0,
            cx: +text.attr('x'),
            cy: +text.attr('y')
        };
    }
    svgElements.each(function (data, index) {
        var svgElement = d3.select(this);
        if (svgElement.attr("cx")) {
            svgElement.attr("cx", roundTo4Decimals(x)).attr("cy", roundTo4Decimals(y));
        } else if (svgElement.attr("points")) {
            var pointsString = svgElement.attr('points');
            svgElement.attr("points", translatePointsAttribute(pointsString, x - bbox.cx, y - bbox.cy));
        } else {
            var d = svgElement.attr('d');
            svgElement.attr("d", translateDAttribute(d, x - bbox.cx, y - bbox.cy));
        }
        if (index == 0 || multiFillShapes.includes(attributes.shape)) {
            svgElement.attr("fill", fill).attr("stroke", stroke);
            if (strokeWidth) {
                svgElement.attr("strokeWidth", strokeWidth);
            }
        }
    });

    if (text.size() != 0) {

        if (textAnchor) {
            text.attr("text-anchor", textAnchor);
        }
        text.attr("x", roundTo4Decimals(+text.attr("x") + x - bbox.cx)).attr("y", roundTo4Decimals(+text.attr("y") + y - bbox.cy));
        if (fontFamily) {
            text.attr("font-family", fontFamily);
        }
        if (fontSize) {
            text.attr("font-size", fontSize);
        }
        if (fontColor) {
            text.attr("fill", fontColor);
        }
        text.text(label);
    }
    return this;
}

function removeDrawnNode() {

    if (!this._drawnNode) {
        return this;
    }

    var node = this._drawnNode.g;

    node.remove();

    this._drawnNode = null;

    return this;
}

function insertDrawnNode(nodeId) {

    if (!this._drawnNode) {
        throw Error('No node has been drawn');
    }

    if (nodeId == null) {
        nodeId = this._drawnNode.nodeId;
    }
    var node = this._drawnNode.g;
    var attributes = this._drawnNode.attributes;

    var title = node.selectWithoutDataPropagation("title");
    title.text(nodeId);
    if (attributes.URL || attributes.tooltip) {
        var ga = node.selectWithoutDataPropagation("g");
        var a = ga.selectWithoutDataPropagation("a");
        var svgElement = a.selectWithoutDataPropagation('ellipse,polygon,path,polyline');
        var text = a.selectWithoutDataPropagation('text');
    } else {
        var svgElement = node.selectWithoutDataPropagation('ellipse,polygon,path,polyline');
        var text = node.selectWithoutDataPropagation('text');
    }
    text.text(attributes.label || nodeId);

    var root = this._selection;
    var svg = root.selectWithoutDataPropagation("svg");
    var graph0 = svg.selectWithoutDataPropagation("g");
    var graph0Datum = graph0.datum();
    var nodeData = this._extractData(node, graph0Datum.children.length, graph0.datum());
    graph0Datum.children.push(nodeData);

    insertAllElementsData(node, nodeData);

    return this;
}

function createNode(nodeId, attributes) {
    var attributesString = '';
    var _iteratorNormalCompletion = true;
    var _didIteratorError = false;
    var _iteratorError = undefined;

    try {
        for (var _iterator = Object.keys(attributes)[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
            var name = _step.value;

            if (attributes[name] != null) {
                attributesString += ' "' + name + '"="' + attributes[name] + '"';
            }
        }
    } catch (err) {
        _didIteratorError = true;
        _iteratorError = err;
    } finally {
        try {
            if (!_iteratorNormalCompletion && _iterator.return) {
                _iterator.return();
            }
        } finally {
            if (_didIteratorError) {
                throw _iteratorError;
            }
        }
    }

    var dotSrc = 'graph {"' + nodeId + '" [' + attributesString + ']}';
    var svgDoc = Viz(dotSrc, { format: 'svg' });
    var parser = new window.DOMParser();
    var doc = parser.parseFromString(svgDoc, "image/svg+xml");
    var newDoc = d3.select(document.createDocumentFragment()).append(function () {
        return doc.documentElement;
    });
    var node = newDoc.select('.node');

    return node;
}

var _graphviz$prototype;

function Graphviz(selection$$1, options$$1) {
    this._options = {
        useWorker: true,
        engine: 'dot',
        totalMemory: undefined,
        keyMode: 'title',
        fade: true,
        tweenPaths: true,
        tweenShapes: true,
        convertEqualSidedPolygons: true,
        tweenPrecision: 1,
        growEnteringEdges: true,
        zoom: true,
        zoomScaleExtent: [0.1, 10],
        zoomTranslateExtent: [[-Infinity, -Infinity], [+Infinity, +Infinity]],
        width: null,
        height: null,
        scale: 1,
        fit: false
    };
    if (options$$1 instanceof Object) {
        var _iteratorNormalCompletion = true;
        var _didIteratorError = false;
        var _iteratorError = undefined;

        try {
            for (var _iterator = Object.keys(options$$1)[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
                var option = _step.value;

                this._options[option] = options$$1[option];
            }
        } catch (err) {
            _didIteratorError = true;
            _iteratorError = err;
        } finally {
            try {
                if (!_iteratorNormalCompletion && _iterator.return) {
                    _iterator.return();
                }
            } finally {
                if (_didIteratorError) {
                    throw _iteratorError;
                }
            }
        }
    } else if (typeof options$$1 == 'boolean') {
        this._options.useWorker = options$$1;
    }
    var useWorker = this._options.useWorker;
    if (typeof Worker == 'undefined') {
        useWorker = false;
    }
    if (useWorker) {
        var scripts = d3.selectAll('script');
        var vizScript = scripts.filter(function () {
            return d3.select(this).attr('type') == 'javascript/worker' || d3.select(this).attr('src') && d3.select(this).attr('src').match(/.*\/viz.js$/);
        });
        if (vizScript.size() == 0) {
            console.warn('No script tag of type "javascript/worker" was found and "useWorker" is true. Not using web worker.');
            useWorker = false;
        } else {
            this._vizURL = vizScript.attr('src');
            if (!this._vizURL) {
                console.warn('No "src" attribute of was found on the "javascript/worker" script tag and "useWorker" is true. Not using web worker.');
                useWorker = false;
            }
        }
    }
    if (useWorker) {
        var js = "\n            onmessage = function(event) {\n                if (event.data.vizURL) {\n                    importScripts(event.data.vizURL);\n                }\n                try {\n                    var svg = Viz(event.data.dot, event.data.options);\n                }\n                catch(error) {\n                    postMessage({\n                        type: \"error\",\n                        error: error.message,\n                    });\n                    return;\n                }\n                if (svg) {\n                    postMessage({\n                        type: \"done\",\n                        svg: svg,\n                    });\n                } else {\n                    postMessage({\n                        type: \"skip\",\n                    });\n                }\n            }\n        ";
        var blob = new Blob([js]);
        var blobURL = window.URL.createObjectURL(blob);
        this._worker = new Worker(blobURL);
    }
    this._selection = selection$$1;
    this._active = false;
    this._busy = false;
    this._jobs = [];
    this._queue = [];
    this._keyModes = new Set(['title', 'id', 'tag-index', 'index']);
    this._images = [];
    this._translation = undefined;
    this._scale = undefined;
    this._eventTypes = ['initEnd', 'start', 'layoutStart', 'layoutEnd', 'dataExtractEnd', 'dataProcessPass1End', 'dataProcessPass2End', 'dataProcessEnd', 'renderStart', 'renderEnd', 'transitionStart', 'transitionEnd', 'restoreEnd', 'end'];
    this._dispatch = d3Dispatch.dispatch.apply(undefined, toConsumableArray(this._eventTypes));
    initViz.call(this);
    selection$$1.node().__graphviz__ = this;
}

function graphviz(selector, options$$1) {
    var g = d3.select(selector).graphviz(options$$1);
    return g;
}

Graphviz.prototype = graphviz.prototype = (_graphviz$prototype = {
    constructor: Graphviz,
    engine: engine,
    addImage: images,
    totalMemory: totalMemory,
    keyMode: keyMode,
    fade: fade,
    tweenPaths: tweenPaths,
    tweenShapes: tweenShapes,
    convertEqualSidedPolygons: convertEqualSidedPolygons,
    tweenPrecision: tweenPrecision,
    growEnteringEdges: growEnteringEdges,
    zoom: zoom$1,
    resetZoom: resetZoom,
    zoomScaleExtent: zoomScaleExtent,
    zoomTranslateExtent: zoomTranslateExtent,
    render: render,
    dot: dot,
    renderDot: renderDot,
    transition: transition$1,
    active: active$1,
    options: options,
    width: width,
    height: height,
    scale: scale,
    fit: fit,
    attributer: attributer,
    on: on,
    onerror: onerror,
    logEvents: logEvents,
    drawEdge: drawEdge,
    updateDrawnEdge: updateDrawnEdge,
    moveDrawnEdgeEndPoint: moveDrawnEdgeEndPoint,
    insertDrawnEdge: insertDrawnEdge,
    removeDrawnEdge: removeDrawnEdge }, defineProperty(_graphviz$prototype, "removeDrawnEdge", removeDrawnEdge), defineProperty(_graphviz$prototype, "drawNode", drawNode), defineProperty(_graphviz$prototype, "updateDrawnNode", updateDrawnNode), defineProperty(_graphviz$prototype, "insertDrawnNode", insertDrawnNode), defineProperty(_graphviz$prototype, "removeDrawnNode", removeDrawnNode), defineProperty(_graphviz$prototype, "removeDrawnNode", removeDrawnNode), _graphviz$prototype);

var selection_graphviz = function (options) {

    var g = this.node().__graphviz__;
    if (g) {
        g.options(options);
        g._dispatch.call("initEnd", this);
    } else {
        g = new Graphviz(this, options);
    }
    return g;
};

var selection_selectWithoutDataPropagation = function (name) {

    return d3.select(this.size() > 0 ? this.node().querySelector(name) : null);
};

d3.selection.prototype.graphviz = selection_graphviz;
d3.selection.prototype.selectWithoutDataPropagation = selection_selectWithoutDataPropagation;

exports.graphviz = graphviz;

Object.defineProperty(exports, '__esModule', { value: true });

})));
