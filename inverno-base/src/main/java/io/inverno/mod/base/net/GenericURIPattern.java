/*
 * Copyright 2021 Jeremy KUHN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inverno.mod.base.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * <p>
 * A generic URI pattern implementation which supports path inclusion check.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see URIPattern
 * @see URIBuilder
 */
class GenericURIPattern implements URIPattern {

	private final String rawValue;
	private final String regex;
	private final List<String> groupNames;
	private final List<SegmentComponent> pathSegments;
	
	private Pattern pattern;
	
	/**
	 * <p>
	 * Creates a generic URI pattern with the specified raw value, regular expression and list of group names.
	 * </p>
	 *
	 * <p>
	 * The resulting won't be able to path inclusion check since no path segments are provided.
	 * </p>
	 *
	 * @param rawValue   a raw value
	 * @param regex      a regular expression
	 * @param groupNames a list of group names
	 */
	public GenericURIPattern(String rawValue, String regex, List<String> groupNames) {
		this(rawValue, regex, groupNames, null);
	}
	
	/**
	 * <p>
	 * Creates a generic URI pattern with the specified raw value, regular
	 * expression and list of group names.
	 * </p>
	 * 
	 * <p>
	 * The resulting instance supports path inclusion check using specified path segments.
	 * </p>
	 * 
	 * @param rawValue   a raw value
	 * @param regex      a regular expression
	 * @param groupNames a list of group names
	 */
	public GenericURIPattern(String rawValue, String regex, List<String> groupNames, List<SegmentComponent> pathSegments) {
		this.rawValue = rawValue;
		this.regex = regex;
		this.groupNames = groupNames != null ? Collections.unmodifiableList(groupNames) : List.of();
		this.pathSegments = pathSegments;
	}
	
	@Override
	public Pattern getPattern() {
		if(this.pattern == null) {
			this.pattern = Pattern.compile(this.regex);
		}
		return this.pattern;
	}

	@Override
	public String getPatternString() {
		return this.regex;
	}

	@Override
	public String getValue() {
		return this.rawValue;
	}
	
	@Override
	public URIMatcher matcher(String uri) {
		return new GenericURIMatcher(this.getPattern().matcher(uri), this.groupNames);
	}
	
	/**
	 * <p>
	 * Checks for path inclusion when path segments were provided or both patterns, returns {@link URIPattern.Inclusion#INDETERMINATE} otherwise.
	 * </p>
	 */
	@Override
	public Inclusion includes(URIPattern pattern) {
		if(!(pattern instanceof GenericURIPattern)) {
			return URIPattern.Inclusion.INDETERMINATE;
		}
		
		GenericURIPattern otherPattern = (GenericURIPattern)pattern;
		
		List<SegmentComponent> p1Segments = this.pathSegments;
		List<SegmentComponent> p2Segments = otherPattern.pathSegments;
		
		if(p1Segments == null || p2Segments == null) {
			// paths segments were nor provided
			return URIPattern.Inclusion.INDETERMINATE;
		}
		
		return this.includes(normalizeSegments(p1Segments), normalizeSegments(p2Segments));
	}
	
	/**
	 * <p>
	 * Checks whether p2Segments is included into p1Segments.
	 * </p>
	 * 
	 * <p>
	 * Lists of segments must be normalized (see {@link #normalizeSegments(java.util.List) }.
	 * </p>
	 * 
	 * @param p1Segments a non null normalized list of segments
	 * @param p2Segments a non null normalized list of segments
	 * 
	 * @return an inclusion state
	 */
	private Inclusion includes(List<SegmentComponent> p1Segments, List<SegmentComponent> p2Segments) {
		int i=0;
		int j=0;
		while(i < p1Segments.size() && j < p2Segments.size()) {
			SegmentComponent s1 = p1Segments.get(i);
			SegmentComponent s2 = p2Segments.get(j);
			
			if(s1.isDirectories() || s1.isCustom()) {
				if(s1.isDirectories()) {
					i++;
				}
				List<SegmentComponent> exitSequence = this.findExitSequence(p1Segments.subList(i, p1Segments.size()));

				// I need to find exitSequence in what's left of p2
 				List<Map.Entry<Integer, Inclusion>> sequenceIndices = this.findSequence(exitSequence, p2Segments.subList(j, p2Segments.size()), s1.isDirectories());
				
				if(!sequenceIndices.isEmpty()) {
					// I have consumed the exit sequence so exitSequence.size() from p1
					List<SegmentComponent> remainingP1 = p1Segments.subList(i + exitSequence.size(), p1Segments.size());

					Inclusion inclusion = Inclusion.DISJOINT;
					for(Map.Entry<Integer, Inclusion> e : sequenceIndices) {
						// we have consumed e.getKey() segments from p2
						List<SegmentComponent> remainingP2 = p2Segments.subList(j + e.getKey() + 1, p2Segments.size());
						
						Inclusion currentInclusion = this.includes(remainingP1, remainingP2);
						if(currentInclusion != Inclusion.DISJOINT) {
							inclusion = e.getValue() == Inclusion.INCLUDED ? currentInclusion : Inclusion.INDETERMINATE;
						}
						
						if(inclusion == Inclusion.INCLUDED) {
							return inclusion;
						}
					}
					return inclusion;
				}
				else {
					// We are disjoint
					return Inclusion.DISJOINT;
				}
			}
			else {
				if(s2.isDirectories() || s2.isCustom()) {
					// we are indeterminate at best
					if(s2.isDirectories()) {
						j++;
					}
					
					List<SegmentComponent> exitSequence = this.findExitSequence(p2Segments.subList(j, p2Segments.size()));
					// I need to find exitSequence in what's left of p1
					List<Map.Entry<Integer, Inclusion>> sequenceIndices = this.findSequence(exitSequence, p1Segments.subList(i, p1Segments.size()), s2.isDirectories());
					
					if(!sequenceIndices.isEmpty()) {
						// I have consumed the exit sequence so exitSequence.size() from p2
						List<SegmentComponent> remainingP2 = p2Segments.subList(j + exitSequence.size(), p2Segments.size());

						for(Map.Entry<Integer, Inclusion> e : sequenceIndices) {
							// we have consumed e.getKey() segments from p1
							List<SegmentComponent> remainingP1 = p1Segments.subList(i + e.getKey() + 1, p1Segments.size());

							if(this.includes(remainingP1, remainingP2) != Inclusion.DISJOINT) {
								return Inclusion.INDETERMINATE;
							}
						}
						return Inclusion.DISJOINT;
					}
					else {
						// We are disjoint
						return Inclusion.DISJOINT;
					}
				}
				else {
					// We have to compare segments
					URIPattern.Inclusion segmentInclusion = s1.includes(s2);
					if(segmentInclusion == URIPattern.Inclusion.INCLUDED) {
						i++;
						j++;
					}
					else {
						// the segment is not included for sure, we can stop here
						// TODO this is not good I can have something indeterminate that is disjoint after
						return segmentInclusion;
					}
				}
			}
		}
		
		// if we get there we have consumed p1, p2 or both
		if(i == p1Segments.size() && j == p2Segments.size()) {
			// We have consumed both, we must have inclusion
			return URIPattern.Inclusion.INCLUDED;
		}
		else if(i == p1Segments.size()) {
			// we consumed p1
			// if it ends with a directories pattern, s1 matches more than s2
			if(i > 0 && p1Segments.get(i-1).isDirectories()) {
				return URIPattern.Inclusion.INCLUDED;
			}
			
			// otherwise we have to check if p2 has segments other than directories patterns or custom patterns
			// custom patterns can match "" which matches p1 as well 
			for(;j<p2Segments.size();j++) {
				SegmentComponent s2 = p2Segments.get(j);
				if(!s2.isDirectories() && !s2.isCustom()) {
					// we know for sure that we are disjoint
					return URIPattern.Inclusion.DISJOINT;
				}
			}
			// p2 matches more than p1
			return URIPattern.Inclusion.INDETERMINATE;
		}
		else {
			// we consumed p2
			if(j > 0 && p2Segments.get(j-1).isDirectories()) {
				// if it ends with a directories pattern, s1 must have only directories pattern for p2 to be included
				// otherwise outcome is indeterminate
				for(;i<p1Segments.size();i++) {
					SegmentComponent s1 = p1Segments.get(i);
					if(!s1.isDirectories()) {
						return URIPattern.Inclusion.INDETERMINATE;
					}
				}
			}
			else {
				// p1 must only contain directories pattern for p2 to be included
				// otherwise p1 ans p2 are disjoint
				for(;i<p1Segments.size();i++) {
					SegmentComponent s1 = p1Segments.get(j);
					if(!s1.isDirectories()) {
						return URIPattern.Inclusion.DISJOINT;
					}
				}
			}
			return URIPattern.Inclusion.INCLUDED;
		}
	}
	
	/**
	 * <p>
	 * Finds a sequence preceded or not by a directories pattern into the specified target.
	 * </p>
	 *
	 * <p>
	 * The sequence must not contain directories or custom patterns and preceded by a directories pattern or a custom pattern that can consumed the target. The directories flag indicate whether a
	 * dierctories pattern directly precedes the sequence (otherwise it has to be a custom pattern).
	 * </p>
	 *
	 * @param sequence
	 * @param target
	 * @param directories true to indicate a preceding directories pattern, false to indicate a perceding custom pattern.
	 *
	 * @return A list of indices of the last segments in the target list of segments where the sequence has been found with corresponding inclusion state: {@link URIPattern.Inclusion#INCLUDED} or
	 *         {@link URIPattern.Inclusion#INDETERMINATE}
	 */
	private List<Map.Entry<Integer, Inclusion>> findSequence(List<SegmentComponent> sequence, List<SegmentComponent> target, boolean directories) {
		if(sequence.isEmpty()) {
			// the sequence is precedeed by a directories pattern and apparently there's no more segment so we match the target for sure
			return List.of(Map.entry(target.size() - 1 , Inclusion.INCLUDED));
		}
		Set<Integer> included_result = new TreeSet<>();
		Set<Integer> indeterminate_result = new TreeSet<>();
		for(int i=0;i<target.size();i++) {
			int inclusion_index = -1;
			Inclusion inclusion = null;
			int match_count = 0;
			for(int j=i,k=0;j<target.size() && k<sequence.size();j++,k++) {
				SegmentComponent t = target.get(j);
				SegmentComponent s = sequence.get(k);
				
				if(t.isDirectories() || t.isCustom()) {
					// Here I am indeterminate at best
					// the only way to have a match would be to have a directories pattern in the sequence at the same position which is not the case
					
					// we need to flip: search the sequence in target before directories pattern or custom pattern into what's remaining of sequence
					// at this point there shouldn't be any directories pattern or custom pattern in both target and sequence.

					Inclusion customSegmentInclusion = null;
					if(t.isCustom()) {
						if(directories && s.isWildcard() && k == 0) {
							// We have .../**/* => /* <-> /{} is then not indeterminate but included (we consume at least one segment)
							customSegmentInclusion = Inclusion.INCLUDED;
						}
						else {
							customSegmentInclusion = s.includes(t);
							if(customSegmentInclusion == Inclusion.DISJOINT) {
								break;
							}
						}
					}
					int remainingTargetIndex = j+1;
					List<SegmentComponent> exitSequence = this.findExitSequence(target.subList(remainingTargetIndex, target.size()));
					List<SegmentComponent> exitSequenceTarget = sequence.subList(k, sequence.size());
					
					List<Map.Entry<Integer, Inclusion>> sequenceIndices = this.findSequence(exitSequence, exitSequenceTarget, t.isDirectories());
					if(!sequenceIndices.isEmpty()) {
						// I have consumed exitSequence.size() segments from the target and n segments from the sequence where n is the index previously returned
						// n is ordered and > 0
						remainingTargetIndex += exitSequence.size();
						List<SegmentComponent> remainingTarget = target.subList(remainingTargetIndex, target.size());
						
						for(Map.Entry<Integer, Inclusion> e : sequenceIndices) {
							int remainingSequenceIndex = k + e.getKey() + 1;
							List<SegmentComponent> remainingSequence = sequence.subList(remainingSequenceIndex, sequence.size());

							if(remainingSequence.isEmpty()) {
								// we consumed the whole sequence
								indeterminate_result.add(remainingTargetIndex - 1);
							}
							else if(remainingTarget.isEmpty()) {
								// we have consumed all target but the sequence is not empty
								// TODO let's leave this for now we'll test and see what happens
							}
							else {
								for(Map.Entry<Integer, Inclusion> e1 : this.findSequence(remainingSequence, remainingTarget, false)) {
									indeterminate_result.add(remainingTargetIndex + e1.getKey());
								}
							}
						}
					}
					if(customSegmentInclusion != null) {
						// we have a custom segment which is either included or indeterminate
						match_count++;
						inclusion_index = j;
						// we have actually found the sequence here already since /{} matches at least one segment
						indeterminate_result.add(inclusion_index - 1);
						if(inclusion != Inclusion.INDETERMINATE) {
							inclusion = customSegmentInclusion;
						}
					}
					else {
						// we have a directories pattern that can be ignored ie. we are included we can continue
						inclusion_index = j;
						// we have actually found the sequence here already since /** matches everything
						indeterminate_result.add(inclusion_index - 1);
						if(inclusion != Inclusion.INDETERMINATE) {
							inclusion = Inclusion.INCLUDED;
						}
					}
				}
				else {
					Inclusion segmentInclusion = s.includes(t);
					if(segmentInclusion == Inclusion.DISJOINT) {
						break;
					}
					else {
						match_count++;
						inclusion_index = j;
						if(inclusion != Inclusion.INDETERMINATE) {
							inclusion = segmentInclusion;
						}
					}
				}
			}
			// I must have consumed all sequence
			if(match_count == sequence.size()) {
				if(inclusion == Inclusion.INCLUDED) {
					included_result.add(inclusion_index);
				}
				else {
					indeterminate_result.add(inclusion_index);
				}
			}
		}
		
		List<Map.Entry<Integer, Inclusion>> result = new ArrayList<>();
		included_result.stream().map(index -> Map.entry(index, Inclusion.INCLUDED)).forEach(result::add);
		indeterminate_result.stream().filter(index -> !included_result.contains(index)).map(index -> Map.entry(index, Inclusion.INDETERMINATE)).forEach(result::add);
		
		return result;
	}
	
	/**
	 * <p>
	 * Normalizes the list of segments.
	 * </p>
	 * 
	 * <p>
	 * A normalized list of segments, is a list where redundant directories pattern segment {@code /**} are removed and appears first in a sequence with wildcard {@code /*} segments. For instance,
	 * <code>/a/{@literal *}/{@literal **}/{@literal *}/{@literal *}/{@literal **}/b/c</code> is normalized to <code>/a/{@literal **}/{@literal *}/{@literal *}/{@literal *}/b/c</code>.
	 * </p>
	 * 
	 * @param segments
	 * @return 
	 */
	private List<SegmentComponent> normalizeSegments(List<SegmentComponent> segments) {
		List<SegmentComponent> result = new ArrayList<>();

		int wildcardIndex = -1;
		boolean inDirectories = false;
		for(SegmentComponent segment : segments) {
			if(segment.isDirectories()) {
				if(!inDirectories) {
					inDirectories = true;
					if(wildcardIndex > 0) {
						result.add(wildcardIndex, segment);
					}
					else {
						result.add(segment);
					}
				}
			}
			else if(segment.isWildcard()) {
				if(wildcardIndex < 0) {
					wildcardIndex = result.size();
				}
				result.add(segment);
			}
			else {
				result.add(segment);
				inDirectories = false;
			}
		}
		return result;
	}
	
	private List<SegmentComponent> findExitSequence(List<SegmentComponent> segments) {
		for(int i=0;i<segments.size();i++) {
			SegmentComponent s = segments.get(i);
			if(s.isDirectories()) {
				return segments.subList(0, i);
			}
			else if(s.isCustom() && i > 0) {
				return segments.subList(0, i);
			}
		}
		return segments;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((groupNames == null) ? 0 : groupNames.hashCode());
		result = prime * result + ((regex == null) ? 0 : regex.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenericURIPattern other = (GenericURIPattern) obj;
		if (groupNames == null) {
			if (other.groupNames != null)
				return false;
		} else if (!groupNames.equals(other.groupNames))
			return false;
		if (regex == null) {
			if (other.regex != null)
				return false;
		} else if (!regex.equals(other.regex))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return this.rawValue;
	}
}
