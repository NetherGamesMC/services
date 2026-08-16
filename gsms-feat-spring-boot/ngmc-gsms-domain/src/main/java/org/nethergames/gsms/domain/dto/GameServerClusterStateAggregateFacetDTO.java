package org.nethergames.gsms.domain.dto;

import java.util.List;

public class GameServerClusterStateAggregateFacetDTO {

	private List<GameServerClusterStateListDTO> results;

	private List<TotalCount> totalCount;

	public List<GameServerClusterStateListDTO> getResults() {
		return results;
	}

	public void setResults(List<GameServerClusterStateListDTO> results) {
		this.results = results;
	}

	public List<TotalCount> getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(List<TotalCount> totalCount) {
		this.totalCount = totalCount;
	}

	public static class TotalCount {
		private long count;

		public long getCount() {
			return count;
		}

		public void setCount(long count) {
			this.count = count;
		}
	}
}
