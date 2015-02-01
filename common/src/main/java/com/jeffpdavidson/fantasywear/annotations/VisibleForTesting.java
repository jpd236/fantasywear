package com.jeffpdavidson.fantasywear.annotations;

/**
 * Annotation indicating that the visibility of a field/method has been increased for testing.
 *
 * Fields and methods annotated with this should be considered private and not used outside the
 * class in which they are declared.
 */
public @interface VisibleForTesting {
}
